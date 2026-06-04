# Semantic Segmentation — UFAL PDI

Semantic segmentation pipeline trained on the Oxford-IIIT Pet dataset and deployed as an Android app.

## What it does

Given a photo of a pet, the app segments the image into three classes:

- **Red** — pet body
- **Blue** — outline/boundary
- **Green** — background

The segmentation mask is displayed alongside the original image in real time on the device.

## Pipeline

1. **Training** — UNet model trained with TensorFlow/Keras on the Oxford-IIIT Pet dataset (`notebooks/train.ipynb`)
2. **Evaluation** — IoU and pixel accuracy metrics computed on a validation subset
3. **Export** — model converted to TFLite (FP32) for mobile deployment
4. **Android app** — Kotlin app using the LiteRT interpreter to run inference on-device

## Repository Structure

```
├── notebooks/
│   └── train.ipynb       # training, evaluation, and TFLite export
├── models/               # trained model files (gitignored)
├── android/              # Android Studio project
│   └── app/src/main/
│       ├── assets/       # model.tflite bundled here
│       └── java/com/ufal/segmentationapp/
│           ├── MainActivity.kt   # UI
│           └── Segmenter.kt      # TFLite inference
├── especifications.md    # course assignment specification
└── requirements.txt      # Python dependencies
```

## Running the training notebook

Requires a CUDA-capable GPU.

```bash
pip install -r requirements.txt
jupyter notebook notebooks/train.ipynb
```

The notebook downloads the dataset automatically, trains for up to 30 epochs, and exports `models/model.tflite`.

## Android app

Built with Android Studio. The model is bundled in `app/src/main/assets/model.tflite`.

1. Open `android/` in Android Studio
2. Build and run on a device or emulator (API 26+)
3. Tap **Selecionar** to pick a photo, then **Segmentar** to run inference
