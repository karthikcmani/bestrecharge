import os

try:
    from pydantic_settings import BaseSettings
except ImportError:
    class BaseSettings:
        pass

class Settings(BaseSettings):
    PROJECT_NAME: str = "BestRecharge Backend API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:///./bestrecharge.db")
    CORS_ORIGINS: list[str] = ["*"]
    ENVIRONMENT: str = os.getenv("ENVIRONMENT", "development")

settings = Settings()
