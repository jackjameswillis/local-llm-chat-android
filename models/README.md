# Models Directory

This directory intentionally excludes large binaries from normal Git history.

Gemma (270M / 1B) GGUF quantized weights are downloaded at runtime into the app’s internal storage.

## LFS Usage
If you decide to store small test weights here:
```bash
git lfs track "*.gguf"
```
Add minimal/placeholder test files (e.g. tiny mock GGUF) for integration testing.

## Production Guidance
- Prefer remote hosting (Hugging Face, internal CDN).
- Include SHA256 checks in downloader before marking models ready.
