Private BGE query embedding assets belong in this directory.

Expected runtime files:

- model.onnx
- vocab.txt

These files are ignored by git. The app uses them to embed user queries with
BAAI/bge-small-en-v1.5 before running vector/hybrid retrieval.
