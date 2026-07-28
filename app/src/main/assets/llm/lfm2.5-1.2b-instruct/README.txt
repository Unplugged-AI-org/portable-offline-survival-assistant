Private local LLM assets belong in this directory.

Expected runtime file:

- LFM2.5-1.2B-Instruct-Q4_K_M.gguf

Use LiquidAI/LFM2.5-1.2B-Instruct-GGUF with Q4_K_M quantization. The model is
loaded through Liquid LEAP from this sideloaded GGUF path. The model file is
ignored by git and should be supplied by a private build step or local developer
setup.

The app records TTFT once the runtime bridge emits the first generated token.
