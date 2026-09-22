# BluetoothCaller

A Bluetooth hands-free calling app prototype built with React.

## Features
- **Pair a Bluetooth device** (headphones, car audio, speaker) via a simulated device scan
- **Dialer** with full keypad
- **Contacts** list with one-tap dial
- **Active call screen** with mute, keypad, speaker, call timer
- **Simulated incoming call** with accept/decline

## Run locally
```bash
npm install
npm run dev
```

## Source
The main app lives in `src/App.jsx` (single-file React component, Tailwind CSS).

> This is a UI prototype — real Bluetooth (Web Bluetooth API) and telephony integration would be the next step.
