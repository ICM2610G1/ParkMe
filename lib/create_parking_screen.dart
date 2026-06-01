import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'login_screen.dart';
import 'map_picker_screen.dart';
import 'dart:io';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';

class CreateParkingScreen extends StatefulWidget {
  const CreateParkingScreen({super.key});

  @override
  State<CreateParkingScreen> createState() => _CreateParkingScreenState();
}

class _CreateParkingScreenState extends State<CreateParkingScreen> {
  List<File> _parkingImages = [];
  final _nameCtrl = TextEditingController();
  final _slotCtrl = TextEditingController();
  final _priceHourCtrl = TextEditingController();
  final _priceMinCtrl = TextEditingController();
  final _fixedPriceCtrl = TextEditingController();
  final _termsCtrl = TextEditingController();

  bool _electricCharges = false;
  String _hourStart = "00:00";
  String _hourFinish = "23:59";
  bool _isLoading = false;

  LatLng? _selectedLocation;
  String _address = "";

  List<bool> _selectedDays = [true, false, true, false, true, false, true];

  Future<void> _selectTime(BuildContext context, bool isStart) async {
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay(
        hour: int.parse(
          isStart ? _hourStart.split(":")[0] : _hourFinish.split(":")[0],
        ),
        minute: int.parse(
          isStart ? _hourStart.split(":")[1] : _hourFinish.split(":")[1],
        ),
      ),
    );
    if (picked != null) {
      setState(() {
        final formattedTime =
            '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
        if (isStart) {
          _hourStart = formattedTime;
        } else {
          _hourFinish = formattedTime;
        }
      });
    }
  }

  Future<void> _createParking() async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) return;

    if (_selectedLocation == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Debes agregar una ubicación en el mapa"),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    const dayLetters = ["L", "M", "M", "J", "V", "S", "D"];
    List<String> activeDays = [];
    for (int i = 0; i < _selectedDays.length; i++) {
      if (_selectedDays[i]) activeDays.add(dayLetters[i]);
    }
    String daysString = activeDays.join(",");

    try {
      List<String> uploadedPhotoUrls = [];

      for (var imageFile in _parkingImages) {
        final ref = FirebaseStorage.instance.ref().child(
          'parking_images/${DateTime.now().millisecondsSinceEpoch}_${_parkingImages.indexOf(imageFile)}.jpg',
        );
        await ref.putFile(imageFile);
        final url = await ref.getDownloadURL();
        uploadedPhotoUrls.add(url);
      }

      await FirebaseFirestore.instance.collection('parking lots').add({
        'operatorId': uid,
        'name': _nameCtrl.text.trim(),
        'pricePerHour': _priceHourCtrl.text.trim(),
        'pricePerMin': _priceMinCtrl.text.trim(),
        'fixedPrice': _fixedPriceCtrl.text.trim(),
        'terms': _termsCtrl.text.trim(),
        'electricCharges': _electricCharges,
        'hourStart': _hourStart,
        'hourFinish': _hourFinish,
        'weekAvailability': daysString,
        'slot': int.tryParse(_slotCtrl.text.trim()) ?? 0,
        'photos': uploadedPhotoUrls,
        'fotos': uploadedPhotoUrls,
        'latitude': _selectedLocation!.latitude,
        'longitude': _selectedLocation!.longitude,
        'address': _address,
        'rate': 0.0,
        'ratingCount': 0,
      });

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Parqueadero creado con éxito"),
          backgroundColor: Colors.green,
        ),
      );
      setState(() {
        _nameCtrl.clear();
        _slotCtrl.clear();
        _priceHourCtrl.clear();
        _priceMinCtrl.clear();
        _fixedPriceCtrl.clear();
        _termsCtrl.clear();
        _selectedLocation = null;
        _address = "";
        _parkingImages.clear();
      });
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Error: $e')));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _pickParkingImages() async {
    final pickedFiles = await ImagePicker().pickMultiImage(imageQuality: 60);
    if (pickedFiles.isNotEmpty) {
      setState(() {
        _parkingImages.addAll(pickedFiles.map((file) => File(file.path)));
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          "Crear parqueadero",
          style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.logout, color: Colors.red),
            onPressed: () async {
              await FirebaseAuth.instance.signOut();
              Navigator.pushReplacement(
                context,
                MaterialPageRoute(builder: (_) => const LoginScreen()),
              );
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            _buildCard("Información General", Icons.home, [
              _buildInput("Nombre del parqueadero", _nameCtrl),
              const SizedBox(height: 12),
              _buildInput("Cupos disponibles", _slotCtrl, TextInputType.number),
              SwitchListTile(
                title: const Text(
                  "Estación de carga EV",
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                activeColor: const Color(0xFF1E88E5),
                contentPadding: EdgeInsets.zero,
                value: _electricCharges,
                onChanged: (val) => setState(() => _electricCharges = val),
              ),
            ]),

            _buildCard("Tarifas (Obligatorio)", Icons.star, [
              Row(
                children: [
                  Expanded(
                    child: _buildInput(
                      "\$ Precio por hora",
                      _priceHourCtrl,
                      TextInputType.number,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _buildInput(
                      "\$ Precio por min",
                      _priceMinCtrl,
                      TextInputType.number,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _buildInput(
                "\$ Tarifa plena (Día completo)",
                _fixedPriceCtrl,
                TextInputType.number,
              ),
            ]),

            _buildCard("Horarios de Atención", Icons.date_range, [
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                      onPressed: () => _selectTime(context, true),
                      child: Text(
                        "Apertura:\n$_hourStart",
                        textAlign: TextAlign.center,
                        style: const TextStyle(color: Colors.black),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                      onPressed: () => _selectTime(context, false),
                      child: Text(
                        "Cierre:\n$_hourFinish",
                        textAlign: TextAlign.center,
                        style: const TextStyle(color: Colors.black),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              const Text(
                "Días de servicio",
                style: TextStyle(color: Colors.grey, fontSize: 14),
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: List.generate(7, (index) {
                  const days = ["L", "M", "M", "J", "V", "S", "D"];
                  return GestureDetector(
                    onTap: () => setState(
                      () => _selectedDays[index] = !_selectedDays[index],
                    ),
                    child: CircleAvatar(
                      radius: 19,
                      backgroundColor: _selectedDays[index]
                          ? const Color(0xFF1E88E5)
                          : const Color(0xFFF0F0F0),
                      child: Text(
                        days[index],
                        style: TextStyle(
                          color: _selectedDays[index]
                              ? Colors.white
                              : Colors.grey,
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                    ),
                  );
                }),
              ),
            ]),

            _buildCard("Fotos del Parqueadero", Icons.camera_alt, [
              ElevatedButton.icon(
                onPressed: _pickParkingImages,
                icon: const Icon(Icons.add_photo_alternate),
                label: const Text("Añadir fotos"),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.grey[200],
                  foregroundColor: Colors.black,
                  elevation: 0,
                ),
              ),
              const SizedBox(height: 12),
              if (_parkingImages.isNotEmpty)
                SizedBox(
                  height: 80,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    itemCount: _parkingImages.length,
                    itemBuilder: (context, index) {
                      return Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: Image.file(
                            _parkingImages[index],
                            width: 80,
                            height: 80,
                            fit: BoxFit.cover,
                          ),
                        ),
                      );
                    },
                  ),
                ),
            ]),

            _buildCard("Detalles y Multimedia", Icons.list, [
              _buildInput("Reglas del parqueadero", _termsCtrl),
              const SizedBox(height: 16),

              InkWell(
                onTap: () async {
                  final result = await Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const MapPickerScreen()),
                  );
                  if (result != null) {
                    setState(() {
                      _selectedLocation = result['latLng'];
                      _address = result['address'];
                    });
                  }
                },
                borderRadius: BorderRadius.circular(16),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: _selectedLocation != null
                        ? const Color(0xFFE8F5E9)
                        : const Color(0xFFF5F7FA),
                    border: Border.all(
                      color: _selectedLocation != null
                          ? const Color(0xFFA5D6A7)
                          : const Color(0xFFE0E0E0),
                    ),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        Icons.location_on,
                        color: _selectedLocation != null
                            ? const Color(0xFF2E7D32)
                            : Colors.grey,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _selectedLocation != null
                                  ? "Ubicación fijada"
                                  : "Ubicación en el mapa",
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                color: Colors.black,
                              ),
                            ),
                            Text(
                              _selectedLocation != null
                                  ? _address
                                  : "Toca para abrir el mapa",
                              style: const TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ]),

            const SizedBox(height: 10),
            _isLoading
                ? const CircularProgressIndicator()
                : ElevatedButton(
                    onPressed: _createParking,
                    child: const Text(
                      "Crear Parqueadero",
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                  ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildCard(String title, IconData icon, List<Widget> children) {
    return Card(
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: const Color(0xFF1E88E5)),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                    color: Colors.black,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildInput(
    String hint,
    TextEditingController ctrl, [
    TextInputType type = TextInputType.text,
  ]) {
    return TextField(
      controller: ctrl,
      keyboardType: type,
      decoration: InputDecoration(
        labelText: hint,
        labelStyle: const TextStyle(color: Colors.grey, fontSize: 13),
        filled: true,
        fillColor: const Color(0xFFF9F9F9),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFF1E88E5)),
        ),
      ),
    );
  }
}
