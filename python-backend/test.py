

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



# import os
# import requests

# api_key = os.getenv("MINIMAX_API_KEY")
# print(api_key)
# resp = requests.get(
#     "https://openrouter.ai/api/v1/models",
#     headers={
#         "Authorization": f"Bearer {api_key}"
#     },
#     timeout=30,
# )

# resp.raise_for_status()

# models = resp.json()["data"]

# target = next(
#     m for m in models
#     if m["id"] == "minimax/minimax-m2.7"
# )

# print("id =", target["id"])
# print("context_length =", target["context_length"])

# from dotenv import load_dotenv
# from langchain_openai import ChatOpenAI


# def main() -> None:
#     load_dotenv()

#     import os

#     api_key = os.getenv("MIMO_API_KEY")
#     model = os.getenv("MIMO_CHAT_MODEL", "mimo-v2.5-pro")
#     base_url = os.getenv("MIMO_BASE_URL", "https://api.xiaomimimo.com/v1")

#     if not api_key or api_key.startswith("your_"):
#         raise RuntimeError("请先在 .env 里配置 MIMO_API_KEY")

#     llm = ChatOpenAI(
#         model=model,
#         api_key=api_key,
#         base_url=base_url,
#         default_headers={"api-key": api_key},
#         temperature=0.2,
#         timeout=60,
#     )

#     response = llm.invoke("agent开发需要掌握什么技能，核心要掌握什么，后端能力很重要还是我的Langgraph能力重要")
#     print(response.content)


# if __name__ == "__main__":
#     main()

import asyncio
from redis.asyncio import Redis

async def main():
    r = Redis(
        host="127.0.0.1",
        port=6379,
        db=0,
        socket_connect_timeout=5,
        socket_timeout=5,
        decode_responses=True,
    )

    try:
        print(await r.ping())
    finally:
        await r.aclose()

asyncio.run(main())