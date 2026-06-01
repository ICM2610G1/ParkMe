import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import 'create_parking_screen.dart';

class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final _nameCtrl = TextEditingController();
  final _lastNameCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();

  bool _isLoading = false;
  File? _profileImage;

  Future<void> _pickImage() async {
    final pickedFile = await ImagePicker().pickImage(
      source: ImageSource.gallery,
      imageQuality: 50,
    );
    if (pickedFile != null)
      setState(() => _profileImage = File(pickedFile.path));
  }

  Future<void> _register() async {
    if (_profileImage == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Por favor selecciona una foto de perfil'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      UserCredential cred = await FirebaseAuth.instance
          .createUserWithEmailAndPassword(
            email: _emailCtrl.text.trim(),
            password: _passCtrl.text.trim(),
          );

      final ref = FirebaseStorage.instance
          .ref()
          .child('profile_images')
          .child('${cred.user!.uid}.jpg');
      await ref.putFile(_profileImage!);
      final imageUrl = await ref.getDownloadURL();

      await FirebaseFirestore.instance
          .collection('users')
          .doc(cred.user!.uid)
          .set({
            'name': _nameCtrl.text.trim(),
            'lastName': _lastNameCtrl.text.trim(),
            'phone': _phoneCtrl.text.trim(),
            'email': _emailCtrl.text.trim(),
            'role': 'Operador',
            'isVerified': true,
            'profileImage': imageUrl,
          });

      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (_) => const CreateParkingScreen()),
        (route) => false,
      );
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Error: $e')));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            const Text(
              "Crea tu Cuenta",
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1E88E5),
              ),
            ),
            const SizedBox(height: 24),

            GestureDetector(
              onTap: _pickImage,
              child: CircleAvatar(
                radius: 50,
                backgroundColor: Colors.grey[300],
                backgroundImage: _profileImage != null
                    ? FileImage(_profileImage!)
                    : null,
                child: _profileImage == null
                    ? const Icon(Icons.camera_alt, size: 40, color: Colors.grey)
                    : null,
              ),
            ),
            const SizedBox(height: 24),

            _buildField("Nombres", _nameCtrl),
            _buildField("Apellidos", _lastNameCtrl),
            _buildField("Teléfono", _phoneCtrl, TextInputType.phone),
            _buildField(
              "Correo electrónico",
              _emailCtrl,
              TextInputType.emailAddress,
            ),
            _buildField(
              "Contraseña",
              _passCtrl,
              TextInputType.visiblePassword,
              true,
            ),
            const SizedBox(height: 32),
            _isLoading
                ? const CircularProgressIndicator()
                : ElevatedButton(
                    onPressed: _register,
                    child: const Text(
                      "Continuar",
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                  ),
          ],
        ),
      ),
    );
  }

  Widget _buildField(
    String hint,
    TextEditingController ctrl, [
    TextInputType type = TextInputType.text,
    bool obscure = false,
  ]) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16.0),
      child: TextField(
        controller: ctrl,
        keyboardType: type,
        obscureText: obscure,
        decoration: InputDecoration(
          hintText: hint,
          filled: true,
          fillColor: const Color(0xFFF9F9F9),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide.none,
          ),
        ),
      ),
    );
  }
}
