Stego Project - Java Steganography + Watermark (merged)

Files:
- StegoApp.java        : Main Java file (AES stego + watermark)
- web/                 : Simple frontend (HTML/CSS/JS) for demo
- server.js            : Optional Node backend to call Java CLI (requires Node)
- README.txt           : This file

How to use (CLI):
1. Compile:
   javac StegoApp.java

2. Hide (encrypt optional):
   java StegoApp hide -in cover.png -data secret.txt -out stego.png -aeskey mypass

3. Extract:
   java StegoApp extract -in stego.png -out recovered.bin -aeskey mypass

4. Embed watermark:
   java StegoApp watermark-embed -in cover.png -out wm.png -text "Owner" -key mykey -redundancy 10

5. Detect watermark:
   java StegoApp watermark-detect -in wm.png -text "Owner" -key mykey -redundancy 10 -th 0.6

Web UI (optional):
- Install Node packages: npm install express multer
- Compile Java: javac StegoApp.java
- Run server: node server.js
- Open: http://localhost:3000

Notes:
- Use PNG images (lossless). JPEG will break LSB stego.
- Keep your AES password safe. If lost, encrypted data cannot be recovered.
