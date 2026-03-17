# 导入核心依赖（新增 Request 和 HTMLResponse 用于自定义 docs）
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from app.api.text_api import router as text_router
from app.config import settings

# 创建 FastAPI 实例
app = FastAPI(
    title="IDEA 插件后端服务",
    description="毕业设计：接收 IDEA 插件文本并处理",
    version="1.0.0",
    debug=settings.DEBUG
)

# 配置CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源，生产环境应该设置具体的前端域名
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有HTTP方法
    allow_headers=["*"],  # 允许所有HTTP头
)

# 注册路由
app.include_router(text_router)

# 根路径测试接口


@app.get("/", summary="健康检查")
async def root():
    return {"message": "FastAPI 服务运行中", "docs_url": "/docs"}


# ========== 关键修复：自定义 docs 路由，使用国内 CDN ==========
@app.get("/docs", response_class=HTMLResponse, include_in_schema=False)
async def custom_swagger_ui_html(request: Request):
    # 改用国内 BootCDN 镜像（稳定可访问）
    # swagger_css = "https://cdn.bootcdn.net/ajax/libs/swagger-ui/5.9.0/swagger-ui.min.css"
    # swagger_bundle_js = "https://cdn.bootcdn.net/ajax/libs/swagger-ui/5.9.0/swagger-ui-bundle.min.js"
    # swagger_standalone_js = "https://cdn.bootcdn.net/ajax/libs/swagger-ui/5.9.0/swagger-ui-standalone-preset.min.js"

    # 替换原有CDN链接，其他代码不变
    swagger_css = "https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui.min.css"
    swagger_bundle_js = "https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui-bundle.min.js"
    swagger_standalone_js = "https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui-standalone-preset.min.js"

    return HTMLResponse(f"""
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
        <meta charset="UTF-8">
        <title>{app.title} - Swagger UI</title>
        <link rel="stylesheet" type="text/css" href="{swagger_css}">
        <style>
            html {{ box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }}
            body {{ margin: 0; padding: 0; }}
        </style>
    </head>
    <body>
        <div id="swagger-ui"></div>
        <script src="{swagger_bundle_js}"></script>
        <script src="{swagger_standalone_js}"></script>
        <script>
            window.onload = function() {{
                SwaggerUIBundle({{
                    url: '{app.openapi_url}',
                    dom_id: '#swagger-ui',
                    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                    layout: "StandaloneLayout",
                    docExpansion: "none",
                    deepLinking: true
                }});
            }}
        </script>
    </body>
    </html>
    """)


# 启动服务的入口
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG  # 调试模式：代码修改自动重启
    )
