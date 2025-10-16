Perfect bro 💪 here’s a **complete `README.md`** you can upload directly to your GitHub repo for your Java Steganography + Watermarking project:

---

# 🖼️ Secure Image Steganography & Watermarking App (Java)

A powerful **Java-based application** that provides **two image security features** in one tool:

1. 🔒 **AES Encrypted Steganography** – Hide and extract secret data inside images securely.
2. 💧 **Invisible Watermarking** – Embed ownership marks into images to detect unauthorized copying.

---

## 🚀 Features

✅ **AES Encryption:** All hidden data is encrypted with a user-provided key before embedding.
✅ **LSB Steganography:** Uses the Least Significant Bit method to embed data invisibly.
✅ **Invisible Watermarking:** Adds a hidden text watermark into RGB channels with redundancy.
✅ **Watermark Detection:** Detects watermark and gives a similarity score.
✅ **Configurable Redundancy:** Increase repetition for stronger watermark detection.
✅ **Web UI (Optional):** Simple frontend using HTML, CSS, and JavaScript to upload and test.
✅ **Cross-Platform:** Works on Windows, Linux, and macOS (Java required).

---

## 🧠 Tech Stack

* **Language:** Java
* **Algorithms:** AES (encryption), LSB (steganography)
* **Frontend:** HTML, CSS, JavaScript (optional)
* **Backend:** Java CLI or Node.js (for web mode)

---

## 📂 Project Structure

```
stego_project/
│
├── StegoApp.java              # Main AES + Watermark Java app
├── README.md                  # Project info (this file)
├── web/
│   ├── index.html             # Web interface
│   ├── style.css              # Styling
│   └── app.js                 # Upload and call backend
├── server.js                  # Node.js backend to execute Java CLI
└── sample/
    ├── cover.png              # Sample input image
    ├── secret.txt             # Sample text file to hide
```

---

## ⚙️ How to Run (Java CLI Mode)

### 1️⃣ Compile:

```bash
javac StegoApp.java
```

### 2️⃣ Run Commands

#### 🔹 Hide (Encrypt + Embed)

```bash
java StegoApp hide -in cover.png -data secret.txt -out stego.png -key myKey123
```

#### 🔹 Extract (Decrypt + Recover)

```bash
java StegoApp extract -in stego.png -out recovered.txt -key myKey123
```

#### 🔹 Embed Watermark

```bash
java StegoApp watermark-embed -in cover.png -out wm.png -text "Owner: Manoj V" -key myKey123 -redundancy 10
```

#### 🔹 Detect Watermark

```bash
java StegoApp watermark-detect -in wm.png -text "Owner: Manoj V" -key myKey123 -th 0.6
```

---

## 🌐 Web Mode (Optional)

1. Install Node.js
2. Run backend server:

   ```bash
   npm install express multer child_process
   node server.js
   ```
3. Open `web/index.html` in browser.

---

## 🧩 Example Output

✅ **Data Hiding:**

```
Data hidden successfully. Encrypted using AES key: myKey123
Output: stego.png
```

✅ **Data Extraction:**

```
Decrypted text saved as recovered.txt
```

✅ **Watermark Detection:**

```
Watermark score: 0.982  (threshold=0.60)  => PRESENT
```

---

## 🛡️ Security Note

All data hidden inside images is **AES-128 encrypted** with your custom key.
Even if someone extracts the bits, they cannot read your data without the key.

---

## 👨‍💻 Author

**Manoj V**
💼 B.Tech Information Technology
💡 Passionate about Cybersecurity, AI, and Blockchain
📍 Chennai, India

---

## ⭐ Contribute & Support

If you find this project useful:

* ⭐ Star the repo on GitHub
* 🐛 Open issues for bugs or ideas
* 🤝 Feel free to fork and improve!

---

## 🏷️ License

This project is open-source under the **MIT License**.
You’re free to use, modify, and share it with credit.

---

Would you like me to also include **screenshots** (sample command output + image before/after) section in this README?
I can generate that markdown part next for you to upload with example images.
