import os
import requests
import json
from openai import OpenAI
from datasets import Dataset
from ragas import evaluate
from ragas.metrics import Faithfulness, AnswerRelevancy, ContextPrecision, ContextRecall
from ragas.llms import llm_factory
from langchain_openai import OpenAIEmbeddings
from test_dataset import test_cases

OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "YOUR_OPENAI_API_KEY")
JWT_TOKEN = os.environ.get("JWT_TOKEN", "Bearer YOUR_JWT_TOKEN")
CHAT_URL = "http://localhost:8082/chat/evaluate"
OUTPUT_FILE = "ragas_baseline_result.json"


def call_chatbot(question: str) -> tuple[str, list[str]]:
    try:
        resp = requests.post(
            CHAT_URL,
            json={"message": question},
            headers={"Authorization": JWT_TOKEN, "Content-Type": "application/json"},
            timeout=60
        )
        resp.raise_for_status()
        data = resp.json()
        return data["answer"], data["contexts"]
    except Exception as e:
        print(f"[ERROR] 챗봇 호출 실패: {e}")
        return "", []


def get_score(val):
    if isinstance(val, list):
        valid = [v for v in val if v is not None]
        return sum(valid) / len(valid) if valid else 0.0
    return val if val is not None else 0.0


def main():
    print("=" * 50)
    print("RAGAS 베이스라인 측정 시작")
    print("=" * 50)

    questions, answers, contexts_list, ground_truths = [], [], [], []

    for i, tc in enumerate(test_cases):
        print(f"[{i+1}/{len(test_cases)}] 질문: {tc['question']}")
        answer, contexts = call_chatbot(tc["question"])
        print(f"         응답: {answer[:80]}{'...' if len(answer) > 80 else ''}")
        print(f"         컨텍스트 수: {len(contexts)}개")

        questions.append(tc["question"])
        answers.append(answer)
        contexts_list.append(contexts if contexts else [tc["reference_context"]])
        ground_truths.append(tc["ground_truth"])

    dataset = Dataset.from_dict({
        "question": questions,
        "answer": answers,
        "contexts": contexts_list,
        "ground_truth": ground_truths,
    })

    print("\nRAGAS 평가 중...")
    openai_client = OpenAI(api_key=OPENAI_API_KEY)
    llm = llm_factory("gpt-4o-mini", client=openai_client)
    embeddings = OpenAIEmbeddings(api_key=OPENAI_API_KEY)

    result = evaluate(
        dataset,
        metrics=[
            Faithfulness(),
            AnswerRelevancy(),
            ContextPrecision(),
            ContextRecall(),
        ],
        llm=llm,
        embeddings=embeddings,
    )

    f_score  = get_score(result['faithfulness'])
    ar_score = get_score(result['answer_relevancy'])
    cp_score = get_score(result['context_precision'])
    cr_score = get_score(result['context_recall'])

    print("\n" + "=" * 50)
    print("📊 RAGAS 베이스라인 결과")
    print("=" * 50)
    print(f"  Faithfulness      : {f_score:.4f}")
    print(f"  Answer Relevancy  : {ar_score:.4f}")
    print(f"  Context Precision : {cp_score:.4f}")
    print(f"  Context Recall    : {cr_score:.4f}")
    print("=" * 50)

    output = {
        "phase": "baseline",
        "faithfulness":      f_score,
        "answer_relevancy":  ar_score,
        "context_precision": cp_score,
        "context_recall":    cr_score,
        "details": [
            {
                "question":     q,
                "answer":       a,
                "ground_truth": g,
                "contexts":     c,
            }
            for q, a, g, c in zip(questions, answers, ground_truths, contexts_list)
        ]
    }

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"\n결과 저장 완료: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()