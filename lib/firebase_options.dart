import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) {
      return web;
    }
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      case TargetPlatform.macOS:
        return macos;
      case TargetPlatform.windows:
        return windows;
      case TargetPlatform.linux:
        throw UnsupportedError(
          'DefaultFirebaseOptions have not been configured for linux - '
          'you can reconfigure this by running the FlutterFire CLI again.',
        );
      default:
        throw UnsupportedError(
          'DefaultFirebaseOptions are not supported for this platform.',
        );
    }
  }

  static const FirebaseOptions web = FirebaseOptions(
    apiKey: 'AIzaSyBy1-hjf37YvPvc56zQWW-TvEcNo9mZTkk',
    appId: '1:1051860002476:web:33629625d7cc9ddd74e109',
    messagingSenderId: '1051860002476',
    projectId: 'parkme-6d96c',
    authDomain: 'parkme-6d96c.firebaseapp.com',
    databaseURL: 'https://parkme-6d96c-default-rtdb.firebaseio.com',
    storageBucket: 'parkme-6d96c.firebasestorage.app',
    measurementId: 'G-1NSS2F2YXL',
  );

  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyD8OhaPGVx_hEAklIlm8aJtssCfI7BOM1Y',
    appId: '1:1051860002476:android:f7fe2836e6a6352274e109',
    messagingSenderId: '1051860002476',
    projectId: 'parkme-6d96c',
    databaseURL: 'https://parkme-6d96c-default-rtdb.firebaseio.com',
    storageBucket: 'parkme-6d96c.firebasestorage.app',
  );

  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'AIzaSyA0JOMPlV9R6HJK1QndCiC7RTyE3r72ngs',
    appId: '1:1051860002476:ios:61e70388bd2b19cd74e109',
    messagingSenderId: '1051860002476',
    projectId: 'parkme-6d96c',
    databaseURL: 'https://parkme-6d96c-default-rtdb.firebaseio.com',
    storageBucket: 'parkme-6d96c.firebasestorage.app',
    iosBundleId: 'com.example.parkmeoperator',
  );

  static const FirebaseOptions macos = FirebaseOptions(
    apiKey: 'AIzaSyA0JOMPlV9R6HJK1QndCiC7RTyE3r72ngs',
    appId: '1:1051860002476:ios:61e70388bd2b19cd74e109',
    messagingSenderId: '1051860002476',
    projectId: 'parkme-6d96c',
    databaseURL: 'https://parkme-6d96c-default-rtdb.firebaseio.com',
    storageBucket: 'parkme-6d96c.firebasestorage.app',
    iosBundleId: 'com.example.parkmeoperator',
  );

  static const FirebaseOptions windows = FirebaseOptions(
    apiKey: 'AIzaSyBy1-hjf37YvPvc56zQWW-TvEcNo9mZTkk',
    appId: '1:1051860002476:web:cadb47f6c65cfab674e109',
    messagingSenderId: '1051860002476',
    projectId: 'parkme-6d96c',
    authDomain: 'parkme-6d96c.firebaseapp.com',
    databaseURL: 'https://parkme-6d96c-default-rtdb.firebaseio.com',
    storageBucket: 'parkme-6d96c.firebasestorage.app',
    measurementId: 'G-JT5W7XM9H9',
  );
}
