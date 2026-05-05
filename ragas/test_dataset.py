test_cases = [
    {
        "question": "반품 신청은 며칠 이내에 해야 하나요?",
        "ground_truth": "상품 수령 후 7일 이내에 반품 신청을 해야 합니다.",
        "reference_context": "상품 수령 후 7일 이내 반품 신청 가능합니다."
    },
    {
        "question": "단순 변심으로 반품할 때 배송비는 누가 내나요?",
        "ground_truth": "단순 변심의 경우 배송비는 구매자가 부담합니다.",
        "reference_context": "단순 변심의 경우 배송비는 구매자 부담입니다."
    },
    {
        "question": "식품은 반품이 가능한가요?",
        "ground_truth": "식품과 신선식품은 반품이 불가합니다.",
        "reference_context": "식품, 신선식품은 반품이 불가합니다."
    },
    {
        "question": "환불은 얼마나 걸리나요?",
        "ground_truth": "반품 상품 수령 확인 후 3영업일 이내에 환불 처리되며, 결제 수단으로 반환됩니다.",
        "reference_context": "반품 상품 수령 확인 후 3영업일 이내 환불 처리됩니다. 환불은 결제 수단으로 반환됩니다."
    },
    {
        "question": "개봉한 화장품도 반품할 수 있나요?",
        "ground_truth": "개봉된 화장품은 반품이 불가합니다.",
        "reference_context": "개봉된 화장품은 반품이 불가합니다."
    },
    {
        "question": "교환 신청은 언제까지 할 수 있나요?",
        "ground_truth": "상품 수령 후 7일 이내에 교환 신청이 가능합니다.",
        "reference_context": "상품 수령 후 7일 이내 교환 신청 가능합니다."
    },
    {
        "question": "상품이 파손되어 왔을 때 교환이 되나요?",
        "ground_truth": "상품 파손 또는 불량은 교환 가능한 사유에 해당합니다.",
        "reference_context": "교환 가능 사유: 상품 파손 또는 불량, 상품 오배송"
    },
    {
        "question": "주문 제작 상품도 교환할 수 있나요?",
        "ground_truth": "주문 제작 상품은 교환이 불가합니다.",
        "reference_context": "교환 불가 상품: 주문 제작 상품"
    },
    {
        "question": "교환 처리는 얼마나 걸리나요?",
        "ground_truth": "교환 상품 수령 확인 후 3영업일 이내에 처리됩니다.",
        "reference_context": "교환 상품 수령 확인 후 3영업일 이내 처리됩니다."
    },
    {
        "question": "잘못 배송된 상품은 반품과 교환 중 어떤 걸 신청해야 하나요?",
        "ground_truth": "오배송은 반품 사유와 교환 가능 사유 모두에 해당하므로, 반품 또는 교환 중 원하는 방법으로 신청할 수 있습니다.",
        "reference_context": "반품 사유: 상품 오배송(WRONG_ITEM). 교환 가능 사유: 상품 오배송"
    },
]