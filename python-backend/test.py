

# from langchain_openai import ChatOpenAI
# from src.app.config.settings import get_settings

# settings = get_settings()

# llm = ChatOpenAI(
#     model=settings.minimax_chat_model,
#     temperature=0.2,
#     api_key=settings.minimax_api_key,
#     base_url=settings.minimax_base_url,
# )

# resp = llm.invoke("你好")
# print(resp.content)


# from sentence_transformers import SentenceTransformer

# model = SentenceTransformer("BAAI/bge-small-zh-v1.5")
# dim = model.get_sentence_embedding_dimension()
# print(dim)   # 一般会是 512

#
#
#
# import asyncio
#
# from fastapi import FastAPI
# from fastapi.responses import StreamingResponse
# import uvicorn
#
# app = FastAPI()
#
#
# async def event_stream():
#     for i in range(1, 6):
#         yield (
#             f"id: {i}\n"
#             f"event: message\n"
#             f"data: hello {i}\n\n"
#         )
#         await asyncio.sleep(1)
#
#
# @app.get("/sse")
# async def sse():
#     return StreamingResponse(
#         event_stream(),
#         media_type="text/event-stream",
#         headers={
#             "Cache-Control": "no-cache",
#             "Connection": "keep-alive",
#         },
#     )
#
#
# if __name__ == "__main__":
#     uvicorn.run(
#         app,
#         host="127.0.0.1",
#         port=8000,
#         log_level="info"
#     )



import os
import requests

api_key = os.getenv("MINIMAX_API_KEY")
print(api_key)
resp = requests.get(
    "https://openrouter.ai/api/v1/models",
    headers={
        "Authorization": f"Bearer {api_key}"
    },
    timeout=30,
)

resp.raise_for_status()

models = resp.json()["data"]

target = next(
    m for m in models
    if m["id"] == "minimax/minimax-m2.7"
)

print("id =", target["id"])
print("context_length =", target["context_length"])