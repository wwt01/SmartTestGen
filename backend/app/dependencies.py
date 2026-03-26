from functools import lru_cache
from fastapi import Depends

from app.services.llm_service import LLMService
from app.services.preprocessing import PreprocessingService
from app.services.text_service import TextService


@lru_cache()
def get_llm_service() -> LLMService:
    return LLMService()


@lru_cache()
def get_preprocessing_service() -> PreprocessingService:
    return PreprocessingService()


def get_text_service(
    llm_service: LLMService = Depends(get_llm_service),
    preprocessing_service: PreprocessingService = Depends(get_preprocessing_service)
) -> TextService:
    return TextService(llm_service, preprocessing_service)
