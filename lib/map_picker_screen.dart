import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:geocoding/geocoding.dart';
import 'dart:ui' as ui;
import 'package:flutter/services.dart';

class MapPickerScreen extends StatefulWidget {
  const MapPickerScreen({super.key});

  @override
  State<MapPickerScreen> createState() => _MapPickerScreenState();
}

class _MapPickerScreenState extends State<MapPickerScreen> {
  LatLng? _pickedLocation;
  String _currentAddress = "Toca en el mapa para ubicar el parqueadero";
  bool _isLoadingAddress = false;

  BitmapDescriptor _customMarkerIcon = BitmapDescriptor.defaultMarker;

  final initialCameraPosition = const CameraPosition(
    target: LatLng(4.6097, -74.0817),
    zoom: 14,
  );

  @override
  void initState() {
    super.initState();
    _loadCustomMarker();
  }

  Future<Uint8List> getBytesFromAsset(String path, int width) async {
    ByteData data = await rootBundle.load(path);
    ui.Codec codec = await ui.instantiateImageCodec(
      data.buffer.asUint8List(),
      targetWidth: width,
    );
    ui.FrameInfo fi = await codec.getNextFrame();
    return (await fi.image.toByteData(
      format: ui.ImageByteFormat.png,
    ))!.buffer.asUint8List();
  }

  Future<void> _loadCustomMarker() async {
    final Uint8List markerIcon = await getBytesFromAsset(
      'assets/pinmaplogo.png',
      120,
    );
    setState(() {
      _customMarkerIcon = BitmapDescriptor.fromBytes(markerIcon);
    });
  }

  Future<void> _getAddress(LatLng position) async {
    setState(() => _isLoadingAddress = true);
    try {
      List<Placemark> placemarks = await placemarkFromCoordinates(
        position.latitude,
        position.longitude,
      );
      if (placemarks.isNotEmpty) {
        Placemark place = placemarks[0];
        setState(() => _currentAddress = "${place.street}, ${place.locality}");
      }
    } catch (e) {
      setState(
        () => _currentAddress =
            "Coordenadas: ${position.latitude}, ${position.longitude}",
      );
    } finally {
      setState(() => _isLoadingAddress = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          "Seleccionar Ubicación",
          style: TextStyle(color: Colors.black, fontSize: 18),
        ),
        backgroundColor: Colors.white,
        iconTheme: const IconThemeData(color: Colors.black),
        elevation: 1,
        actions: [
          if (_pickedLocation != null)
            TextButton(
              onPressed: () => Navigator.pop(context, {
                'latLng': _pickedLocation,
                'address': _currentAddress,
              }),
              child: const Text(
                "Confirmar",
                style: TextStyle(
                  color: Color(0xFF1E88E5),
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ),
        ],
      ),
      body: Stack(
        children: [
          GoogleMap(
            initialCameraPosition: initialCameraPosition,
            onTap: (position) {
              setState(() => _pickedLocation = position);
              _getAddress(position);
            },
            markers: _pickedLocation == null
                ? {}
                : {
                    Marker(
                      markerId: const MarkerId("m1"),
                      position: _pickedLocation!,
                      icon: _customMarkerIcon,
                    ),
                  },
          ),
          Positioned(
            bottom: 40,
            left: 20,
            right: 20,
            child: Card(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(15),
              ),
              elevation: 4,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  children: [
                    const Icon(
                      Icons.location_on,
                      color: Color(0xFF1E88E5),
                      size: 30,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _isLoadingAddress
                          ? const Text("Buscando dirección...")
                          : Text(
                              _currentAddress,
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
