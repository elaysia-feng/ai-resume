import asyncio
import os
from pathlib import Path

from dotenv import load_dotenv
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain_core.messages import HumanMessage, SystemMessage

load_dotenv()

PROJECT_ROOT = str(Path(__file__).resolve().parents[2])


async def get_mcp_tools():
    client = MultiServerMCPClient(
        {
            "minimax_coding_plan": {
                "transport": "stdio",
                "command": "uvx",
                "args": ["minimax-coding-plan-mcp", "-y"],
                "env": {
                    "MINIMAX_API_KEY": os.getenv("MINIMAX_API_KEY"),
                    # MCP 建议不要用 /v1
                    "MINIMAX_API_HOST": os.getenv(
                        "MINIMAX_API_HOST",
                        "https://api.minimaxi.com",
                    ),
                },
            },
            "minimax": {
                "transport": "stdio",
                "command": "minimax-mcp",
                "args": [],
                "env": {
                    "MINIMAX_API_KEY": os.getenv("MINIMAX_API_KEY"),
                    "MINIMAX_API_HOST": os.getenv(
                        "MINIMAX_API_HOST",
                        "https://api.minimaxi.com",
                    ),
                    "MINIMAX_MCP_BASE_PATH": PROJECT_ROOT,
                    "MINIMAX_API_RESOURCE_MODE": "url",
                },
            },
        }
    )

    tools = await client.get_tools()

    print(f"已加载工具（{len(tools)} 个）：")
    for tool in tools:
        print("-", tool.name)

    return tools