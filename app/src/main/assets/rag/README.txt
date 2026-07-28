Generated private RAG corpus databases belong in this directory.

Expected runtime asset:

- rag/posa_rag.db

Build it locally from the ignored private corpus:

rag_source_corpus/.venv/bin/python rag_source_corpus/ingestion/export_rag_asset_db.py

Do not commit generated .db/.sqlite files from this directory.
