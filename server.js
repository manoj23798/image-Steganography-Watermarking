// Minimal Node backend that calls Java CLI (optional).
// Requires: npm install express multer
const express = require('express');
const multer  = require('multer');
const fs = require('fs');
const cp = require('child_process');
const upload = multer({ dest: 'tmp/' });
const app = express();

app.post('/hide', upload.fields([{name:'image'},{name:'data'}]), (req, res) => {
  try {
    const image = req.files['image'][0].path;
    const data = req.files['data'][0].path;
    const out = 'tmp/out_stego.png';
    const aes = req.body.aeskey ? ['-aeskey', req.body.aeskey] : [];
    const cmd = ['java','StegoApp','hide','-in',image,'-data',data,'-out',out].concat(aes).join(' ');
    cp.execSync(cmd, {stdio:'inherit'});
    res.download(out, 'stego.png', err => cleanupFiles([image,data,out]));
  } catch (e) { res.status(500).send('Error: ' + e.toString()); }
});

app.post('/extract', upload.single('image'), (req, res) => {
  try {
    const image = req.file.path;
    const out = 'tmp/out_extracted.bin';
    const aes = req.body.aeskey ? ['-aeskey', req.body.aeskey] : [];
    const cmd = ['java','StegoApp','extract','-in',image,'-out',out].concat(aes).join(' ');
    cp.execSync(cmd, {stdio:'inherit'});
    res.download(out, 'extracted.bin', err => cleanupFiles([image,out]));
  } catch (e){ res.status(500).send('Error: '+e.toString()); }
});

app.post('/watermark-embed', upload.single('image'), (req, res) => {
  try {
    const image = req.file.path;
    const out = 'tmp/out_wm.png';
    const text = req.body.text;
    const key = req.body.key;
    const red = req.body.redundancy || '5';
    const cmd = ['java','StegoApp','watermark-embed','-in',image,'-out',out,'-text',`"${text}"`,'-key',key,'-redundancy',red].join(' ');
    cp.execSync(cmd, {stdio:'inherit'});
    res.download(out, 'wm.png', err => cleanupFiles([image,out]));
  } catch (e) { res.status(500).send('Error: '+e.toString()); }
});

app.post('/watermark-detect', upload.single('image'), (req, res) => {
  try {
    const image = req.file.path;
    const text = req.body.text;
    const key = req.body.key;
    const red = req.body.redundancy || '5';
    const cmd = ['java','StegoApp','watermark-detect','-in',image,'-text',`"${text}"`,'-key',key,'-redundancy',red,'-th','0.60'].join(' ');
    let out = cp.execSync(cmd, {encoding:'utf8'});
    cleanupFiles([image]);
    res.type('text/plain').send(out);
  } catch (e) { res.status(500).send('Error: '+e.toString()); }
});

function cleanupFiles(list){ for(const f of list) if(fs.existsSync(f)) fs.unlinkSync(f); }

app.use(express.static('web'));
app.listen(3000, ()=> console.log('Server running http://localhost:3000'));
