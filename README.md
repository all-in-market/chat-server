# 🖥️ 채팅 서버

<br>

---

# 1. 📌 서버 개요

## 서버 소개

 채팅 서버는 멀티벤더 마켓플레이스 플랫폼의 채팅 기능을 담당하는 서버입니다. 
 구매자와 판매자 간 WebSocket 기반 실시간 채팅과, 반품·교환 정책 문서를 기반으로 고객 질문에 답변하는 RAG 기반 AI 고객 어시스턴트를 제공합니다.

<br>

---

# 2. 📡 주요 API

| Method | URI              | Description       |
|--------|------------------|-------------------|
| POST   | /chat/stream     | 챗봇 대화 API         |
| POST   | /chat/rooms      | 채팅방 생성 API        |
| GET    | /chat/rooms | 채팅방 목록 조회 API     |
| GET    | /chat/rooms/{roomId}/messages | 채팅 내역 조회(스크롤) API |

<br>

---

# 3. 🔄 서비스 플로우

## WebSocket 인증 플로우

```mermaid
sequenceDiagram
    autonumber

    participant Client
    participant Interceptor as JwtChannelInterceptor
    participant JWT as JwtProvider
    participant WS as WebSocket

    Client->>WS: CONNECT + JWT Token

    WS->>Interceptor: preSend()

    Interceptor->>JWT: validateToken()

    alt 인증 성공
        JWT-->>Interceptor: Valid Token

        Interceptor->>Interceptor: Principal 생성

        Interceptor-->>WS: 연결 허용

    else 인증 실패
        JWT-->>Interceptor: Invalid Token

        Interceptor-->>Client: 연결 거부
    end
```

<br>

---

## 실시간 메시지 전송 플로우

```mermaid
sequenceDiagram
    autonumber

    participant Client
    participant WS as WebSocket Controller
    participant Facade as RealtimeChatFacade
    participant Service as RealtimeChatService
    participant Redis as Redis Pub/Sub
    participant Subscriber as RedisSubscriber
    participant Other as Other Clients

    Client->>WS: /pub/chat/message

    WS->>Facade: handleMessage()

    Facade->>Service: validateParticipant()

    Facade->>Service: save()

    Service->>DB: 메시지 저장

    Facade->>Service: updateLastMessage()

    Service->>DB: 채팅방 마지막 메시지 갱신

    Facade->>Service: getParticipantIds()

    Note right of Facade: afterCommit()

    Facade->>Redis: publish()

    Note right of Redis: chat.room.{roomId}

    Redis-->>Subscriber: Pub/Sub 전달

    Subscriber->>Other: /sub/chat/room/{roomId}

    Other-->>Other: 실시간 메시지 수신

    Facade->>Client: ACK SUCCESS
```

<br>

---

## Redis Pub/Sub 멀티 서버 구조

```mermaid
flowchart LR

    A[Client A] --> B[Chat Server 1]

    C[Client B] --> D[Chat Server 2]

    B --> E[Redis Pub/Sub]

    D --> E

    E --> B

    E --> D

    B --> A

    D --> C

    subgraph Multi Server Environment
        B
        D
        E
    end
```

<br>

---

## 읽음 처리 플로우

```mermaid
sequenceDiagram
    autonumber

    participant Client
    participant WS as WebSocket
    participant Service as RealtimeChatService
    participant DB
    participant Redis
    participant Subscriber
    participant Other as Other Clients

    Client->>WS: /pub/chat/read

    WS->>Service: read()

    Service->>DB: lastReadMessageId 저장

    Note right of Service: afterCommit()

    Service->>Redis: unread reset

    Service->>Redis: publishRead()

    Redis-->>Subscriber: read 이벤트 전달

    Subscriber->>Other: /sub/chat/read/{roomId}

    Other-->>Other: 읽음 상태 실시간 반영
```

<br>

---

## Unread Count 캐시 전략

```mermaid
flowchart TD

    A[Unread Count 요청] --> B{Redis Cache 존재 여부}

    B -- HIT --> C[Redis 값 반환]

    B -- MISS --> D[DB unread count 조회]

    D --> E[Redis 저장]

    E --> F[Unread Count 반환]
```

<br>

---

## 채팅방 생성 동시성 처리

```mermaid
flowchart TD

    A[채팅방 생성 요청] --> B[기존 채팅방 조회]

    B --> C{채팅방 존재 여부}

    C -- YES --> D[기존 채팅방 반환]

    C -- NO --> E[채팅방 생성 시도]

    E --> F{Unique Constraint 발생 여부}

    F -- NO --> G[채팅방 생성 성공]

    F -- YES --> H[기존 채팅방 재조회]

    H --> I[이미 생성된 채팅방 반환]
```

<br>

---

## ACK 기반 메시지 상태 동기화

```mermaid
sequenceDiagram
    autonumber

    participant Client
    participant Facade as RealtimeChatFacade
    participant Redis
    participant WS as WebSocket

    Client->>Facade: 메시지 전송(tempId 포함)

    Facade->>Redis: publish()

    alt 발행 성공
        Facade->>WS: ACK SUCCESS
    else 발행 실패
        Facade->>WS: ACK REDIS_PUBLISH_FAILED
    end

    WS-->>Client: tempId 기반 상태 동기화
```

<br>

---

## 전체 AI 챗봇 아키텍처

```mermaid
flowchart TB

    Client[사용자 Client]
    ChatController[ChatController]
    Moderation[ModerationService]
    Intent[IntentClassifier]
    AiAssistant[AiAssistant]
    Retriever[HybridContentRetriever]
    Vector[Vector Search]
    Keyword[Keyword Search]
    RRF[RRF Rank Fusion]
    PgVector[(PGVector)]
    Redis[(Redis Chat Memory)]
    OpenAI[OpenAI GPT-4o-mini]
    ApiServer[API Server]
    Jwt[JWT 인증]
    SSE[SSE Streaming]

    Client -->|POST /chat/stream| Jwt
    Jwt --> ChatController

    ChatController --> Moderation
    Moderation -->|정상 요청| Intent

    Intent -->|SMALL_TALK| AiAssistant
    Intent -->|INQUIRY| Retriever

    Retriever --> Vector
    Retriever --> Keyword

    Vector --> PgVector
    Keyword --> PgVector

    Vector --> RRF
    Keyword --> RRF

    RRF --> AiAssistant

    AiAssistant --> Redis
    AiAssistant --> OpenAI

    AiAssistant -->|Tool 호출| ApiServer

    OpenAI --> ChatController
    ChatController --> SSE
    SSE --> Client
```

<br>

---

## 채팅 요청 처리 흐름

```mermaid
sequenceDiagram

    autonumber

    participant User
    participant Filter as JwtAuthenticationFilter
    participant Controller as ChatController
    participant Moderation as ModerationService
    participant Intent as IntentClassifier
    participant Retriever as HybridContentRetriever
    participant Assistant as AiAssistant
    participant OpenAI
    participant Redis
    participant API as API Server

    User->>Filter: Authorization JWT 포함 요청

    Filter->>Filter: JWT 검증
    Filter->>Controller: 인증 완료

    Controller->>Moderation: 유해성 검사

    alt 유해 콘텐츠
        Moderation-->>Controller: flagged=true
        Controller-->>User: 차단 메시지 반환
    else 정상 요청
        Moderation-->>Controller: flagged=false

        Controller->>Intent: 의도 분류

        alt SMALL_TALK
            Intent-->>Controller: SMALL_TALK
            Controller->>Assistant: smallTalk()
        else INQUIRY
            Intent-->>Controller: INQUIRY

            Controller->>Retriever: retrieve(query)

            par Hybrid Search
                Retriever->>Retriever: Vector Search
                Retriever->>Retriever: Keyword Search
            end

            Retriever->>Retriever: RRF 점수 병합

            Retriever-->>Controller: Context 반환

            Controller->>Assistant: chat()
        end

        Assistant->>Redis: 대화 메모리 조회
        Assistant->>OpenAI: Prompt + Context 전달

        opt Tool Calling
            Assistant->>API: 주문/상품/반품 API 호출
        end

        OpenAI-->>Assistant: Streaming Token

        loop Streaming
            Assistant-->>Controller: Token Chunk
            Controller-->>User: SSE Stream
        end

        Assistant->>Redis: 대화 저장
    end
```

<br>

---

## RAG 검색 구조

```mermaid
flowchart LR

    Query[사용자 질문]

    Query --> VectorSearch[Vector Search]
    Query --> KeywordSearch[Keyword Search]

    VectorSearch --> EmbeddingModel[text-embedding-3-small]
    EmbeddingModel --> PgVector[(PGVector)]

    KeywordSearch --> SQL[ILIKE 검색]

    PgVector --> RRF
    SQL --> RRF

    RRF[Reciprocal Rank Fusion]

    RRF --> TopK[상위 3개 Context]
    TopK --> GPT[GPT-4o-mini]
```

<br>

---

## 문서 인제스천 파이프라인

```mermaid
flowchart TB

    Policy[정책 문서 txt]

    Policy --> Loader[FileSystemDocumentLoader]

    Loader --> Splitter[SemanticDocumentSplitter]

    Splitter --> Sentence[문장 단위 분리]

    Sentence --> Embedding[text-embedding-3-small]

    Embedding --> Similarity[문장 간 Cosine Similarity 계산]

    Similarity --> Chunking[Semantic Chunk 생성]

    Chunking --> Vectorize[Embedding 생성]

    Vectorize --> PGVector[(PGVector 저장)]
```

<br>

---

## Semantic Chunking 내부 구조

```mermaid
flowchart TD

    Start[문서 입력]

    Start --> SentenceSplit[문장 단위 분리]

    SentenceSplit --> Embedding[문장별 임베딩 생성]

    Embedding --> Similarity[인접 문장 유사도 계산]

    Similarity --> Decision{Threshold 0.7 이상인가?}

    Decision -->|YES| Merge[현재 Chunk에 추가]
    Decision -->|NO| NewChunk[새 Chunk 생성]

    Merge --> Similarity
    NewChunk --> Similarity

    Similarity --> Final[최종 Semantic Chunk 반환]
```

<br>

---

## Hybrid Search + RRF 구조

```mermaid
flowchart TB

    Query[사용자 질문]

    Query --> Vector[Vector Retriever]
    Query --> Keyword[Keyword Retriever]

    Vector --> VectorRank[벡터 검색 순위]
    Keyword --> KeywordRank[키워드 검색 순위]

    VectorRank --> RRF
    KeywordRank --> RRF

    RRF[점수 계산]

    RRF --> Merge[점수 병합]

    Merge --> Sort[최종 정렬]

    Sort --> Result[Top 3 Context]
```

<br>

---

## SSE 스트리밍 구조

```mermaid
sequenceDiagram

    autonumber

    participant User
    participant Controller as ChatController
    participant Executor as Virtual Thread
    participant GPT as OpenAI Streaming API

    User->>Controller: /chat/stream 요청

    Controller->>Executor: 가상 스레드 실행

    Executor->>GPT: Streaming 요청

    loop Token Streaming
        GPT-->>Controller: Partial Token
        Controller-->>User: SSE Chunk 전송
    end

    GPT-->>Controller: Complete

    Controller-->>User: SSE 종료
```

<br>

---

## Redis 기반 Chat Memory 구조

```mermaid
flowchart LR

    User[사용자]
    GPT[GPT-4o-mini]
    Memory[MessageWindowChatMemory]
    Redis[(Redis)]

    User --> GPT

    GPT --> Memory

    Memory --> Redis

    Redis --> Memory

    Memory --> GPT
```

<br>

---

## Virtual Thread 기반 비동기 처리

```mermaid
flowchart TB

    Request[채팅 요청]

    Request --> TaskExecutor

    TaskExecutor --> VirtualThread[Virtual Thread 생성]

    VirtualThread --> SecurityContext[SecurityContext 복제]

    SecurityContext --> AI[AI Streaming 처리]

    AI --> SSE[SSE 응답]
```

<br>

---

## Tool Calling 구조

```mermaid
flowchart LR

    GPT[GPT-4o-mini]

    GPT --> Tool[ChatTools]

    Tool --> Orders[getOrders]
    Tool --> Order[getOrder]
    Tool --> Products[searchProducts]
    Tool --> Product[getProduct]
    Tool --> Refund[createRefund]

    Orders --> ApiServer
    Order --> ApiServer
    Product --> ApiServer
    Refund --> ApiServer
```

<br>

---

## RAGAS 평가 파이프라인

```mermaid
flowchart TB

    TestCase[Test Questions]

    TestCase --> Chatbot(/chat/evaluate)

    Chatbot --> Answer[AI Answer]
    Chatbot --> Contexts[Retrieved Contexts]

    Answer --> RAGAS
    Contexts --> RAGAS

    GroundTruth[Ground Truth] --> RAGAS

    RAGAS --> Metrics[
      Faithfulness
      Answer Relevancy
      Context Precision
      Context Recall
    ]

    Metrics --> Report[HTML Report]
```

<br>

---

## OpenAI 모델 구성

```mermaid
flowchart LR

    GPT[gpt-4o-mini]
    Moderation[omni-moderation-latest]
    Embedding[text-embedding-3-small]

    GPT --> ChatResponse[채팅 응답 생성]

    Moderation --> Unsafe[유해 콘텐츠 감지]

    Embedding --> Vector[1536 차원 임베딩 생성]
```

<br>

---

# 4. 🗂️ ERD

![ERD](/docs/image/ChattingServerERD.png)

<br>

---

# 5. 🧠 기술적 의사 결정

## WebSocket + Redis Pub/Sub 활용

### 배경

멀티 벤더 이커머스 환경에서 구매자와 판매자 간 실시간 채팅 기능이 필요했다. 
일반적인 HTTP 방식은 클라이언트가 새로운 메시지를 확인하려면 지속적으로 서버에 요청해야 하고, 사용자 수 증가 시 서버 부하가 커지는 문제가 있었다. 
특히 여러 구매자와 판매자가 동시에 채팅하고 주문·배송 문의가 실시간으로 발생하는 환경에서는 단순 HTTP Polling 방식이 비효율적이었다.

### 기술 선택지

| 비교 항목  |HTTP Polling| SSE              |WebSocket + STOMP|
|--------|------------|------------------|------------------|
| 통신 방식  |단방향 (클라이언트 → 서버)| 단방향 (서버 → 클라이언트) |양방향|
| 실시간성   |낮음| 중간               |높음|
| 서버 부하  |높음 (반복 요청)| 낮음               |낮음|
| 채팅 적합성 |부적합| 부적합              |적합|

| 비교 항목 | Spring SimpleBroker | Redis Pub/Sub   |
|-------|---------------------|-----------------|
| 세션 관리 | 서버 메모리              |중앙 브로커|
| 멀티 서버 대응| 불가                  |가능|
|수평 확장| 불가          |가능|

### 선택 이유

HTTP Polling은 불필요한 반복 요청이 발생하고, SSE는 서버→클라이언트 단방향 통신만 가능하다. 
WebSocket은 한 번 연결 후 양방향 통신이 유지되어 낮은 지연 시간과 실시간 Push가 가능해 채팅 구조에 적합하다.

Spring SimpleBroker는 세션을 서버 메모리에서만 관리하기 때문에, ECS Fargate 기반 수평 확장 환경에서 서버 A에 연결된 사용자의 메시지가 서버 B에 연결된 사용자에게 전달되지 않는 문제가 발생한다. 
Redis Pub/Sub을 중앙 브로커로 두면 어느 서버에서 메시지를 발행하더라도 모든 인스턴스가 수신하고 자신의 세션에 연결된 클라이언트에게만 전달할 수 있어, 멀티 서버 환경의 메시지 동기화 문제를 구조적으로 해결할 수 있다.

### 해결 및 결과

WebSocket 도입으로 실시간 양방향 채팅 기능을 구현했고, Redis Pub/Sub 도입으로 멀티 서버 환경에서의 메시지 동기화 문제를 해결하며 ECS Fargate 기반 수평 확장이 가능한 구조를 갖췄다. 
ACK 응답과 읽지 않은 메시지(unread) 처리, Redis 기반 메시지 브로드캐스트를 구현해 실제 서비스 수준의 실시간 채팅 구조를 완성했다.

<br>

---

## 도메인 특화 Narrow RAG 전략 채택

### 배경

멀티벤더 마켓플레이스의 고객 문의 챗봇을 구축하는 상황에서, 교환/환불/배송 등 정책 관련 질문에 정확한 답변을 제공해야 했다.

### 기술 선택지

| 비교 항목 | Wide & Diverse RAG | Narrow & Specialized RAG |
|---|---|---|
| 지식 범위 | 위키, 뉴스, 블로그 등 광범위한 데이터 | 검증된 도메인 문서만 사용 |
| 환각(Hallucination) 위험 | 높음 | 낮음 |
| 출처 검증 | 어려움 | 명확함 |
| 법적 분쟁 대응 | 어려움 | 구조적 차단 가능 |

### 선택 이유

Wide RAG는 다양한 질문에 답할 수 있지만, 정책처럼 정확성이 중요한 도메인에서는 환각(Hallucination) 위험이 높고 출처 검증이 어렵다. 
반면 검증된 정책 문서만을 사용하면 답변의 출처가 명확해지고, 법적 분쟁 소지를 구조적으로 차단할 수 있다.

### 해결 및 결과

지식 베이스를 정책 문서로 한정함으로써 LLM이 학습 지식으로 추측하는 상황 자체를 제거했다. 
정책에 없는 질문은 답변하지 않는 명확한 경계가 생겨 답변 일관성과 신뢰도가 높아졌다.

<br>

---

## 하이브리드 검색과 RRF 알고리즘으로 검색 품질 고도화

### 배경

RAG 파이프라인에서 사용자 질문과 관련된 청크를 검색하는 단계가 필요했다.

### 기술 선택지

| 비교 항목 | 벡터 검색 | 키워드 검색 | 하이브리드 검색 + RRF |
|---|---|---|---|
| 검색 방식 | 의미 기반 | 정확한 단어 매칭 | 두 방식 통합 |
| 단점 | 관련 없는 청크 포함 가능 | 동의어·문맥 누락 | - |
| 상호 보완 | - | - | 각 방식의 단점 보완 |

### 선택 이유

벡터 검색만 사용하면 의미적으로 유사하지만 실제로는 관련 없는 청크가 결과에 포함되고, 키워드 검색만 사용하면 동의어나 문맥을 놓친다. 
Cormack et al. (2009) 논문(SIGIR 2009)에서 검증된 RRF 알고리즘은 두 검색 방식의 순위를 통합해 각각의 단점을 상호 보완한다.

### 해결 및 결과

학술적으로 검증된 RRF 알고리즘을 직접 구현해 두 검색 방식의 장점을 결합했고, 이를 통해 검색 정밀도를 높여 LLM에 전달되는 컨텍스트 품질을 개선했다.

<br>

---

## Java 21 Virtual Thread로 동시성 처리 최적화

### 배경

RAG 파이프라인은 벡터 검색, 키워드 검색, LLM 호출 등 여러 I/O 집약적 작업이 순차적으로 또는 병렬로 실행된다.

### 기술 선택지

| 비교 항목 | ThreadPool | Virtual Thread |
|---|---|---|
| 스레드 수 | maxPoolSize로 고정 | 제한 없음 |
| 생성 비용 | 높음 | 낮음 |
| I/O 대기 처리 | 스레드 점유 | carrier thread 반납 후 다른 작업 실행 |
| I/O 집약 워크로드 적합성 | 낮음 | 높음 |

### 선택 이유

ThreadPool은 maxPoolSize가 고정되어 있어 동시 요청이 한계를 초과하면 이후 요청이 대기 상태에 빠진다. 
Virtual Thread는 OS 스레드와 1:1로 매핑되지 않아 생성 비용이 낮고, I/O 대기 중에는 carrier thread를 반납해 다른 작업이 실행될 수 있다. 
RAG처럼 I/O 비중이 높은 워크로드에 구조적으로 적합하다.

### 해결 및 결과

Virtual Thread 도입으로 동시성 제한을 제거했고, SecurityContext 전파 문제를 직접 해결하는 과정에서 Virtual Thread의 동작 방식을 깊이 이해하게 됐다. 
I/O 집약적 워크로드에서 ThreadPool을 대체하는 적합한 선택이었다.

<br>

---

## Semantic Chunking으로 의미 단위 문서 분할

### 배경

정책 문서를 벡터 DB에 저장하기 위해 청킹(분할) 작업이 필요했다.

### 기술 선택지

| 비교 항목 | 고정 크기 청킹 | Semantic Chunking |
|---|---|---|
| 분할 기준 | 토큰 수 또는 문자 수 | 문장 간 임베딩 유사도 |
| 의미 구조 보존 | 무시 | 자연스러운 경계에서 분할 |
| 정책 항목 분리 문제 | 발생 가능 | 구조적으로 방지 |

### 선택 이유

고정 크기 청킹은 문서의 의미 구조를 무시하고 자르기 때문에, 하나의 정책 항목이 두 청크에 걸쳐 분리되는 문제가 발생한다. 
Reimers & Gurevych (2019) 논문(EMNLP 2019)에서 제안한 임베딩 유사도 기반 분할은 인접 문장 간 의미 연속성을 판단해 자연스러운 경계에서만 분할한다.

### 해결 및 결과

의미 단위 분할을 통해 청크의 완결성을 확보했고, 검색 시 불완전한 컨텍스트가 전달되는 문제를 구조적으로 해결했다. 
청킹 전략 하나가 검색 품질 전체에 미치는 영향을 직접 확인했다.

<br>

---

## Adaptive RAG로 질문 유형별 처리 전략 분기

### 배경

챗봇에 들어오는 질문은 정책 관련 질문만이 아니다. "안녕하세요", "감사합니다" 같은 일상적인 인사도 포함된다.

### 기술 선택지

| 비교 항목 | 단일 전략 | Adaptive RAG |
|---|---|---|
| 처리 방식 | 모든 질문에 RAG 파이프라인 적용 | 질문 유형 분류 후 처리 방식 분기 |
| 불필요한 검색 호출 | 발생 | 제거 |
| 자원 효율 | 낮음 | 높음 |

### 선택 이유

검색이 필요 없는 인사말에도 벡터 검색과 키워드 검색을 수행하면 불필요한 자원이 소비되고 응답 시간이 늘어난다. 
Jeong et al. (2024) 논문(NAACL 2024)은 질문의 복잡도에 따라 검색 전략을 달리해야 한다는 것을 보여준다.

### 해결 및 결과

질문 유형별로 처리 전략을 분기함으로써 불필요한 검색 호출을 제거하고 자원 효율을 높였다. 
모든 질문을 동일하게 처리하는 것이 항상 최선이 아님을 보여주는 설계 결정이었다.

<br>

---

# 6. 🚨 트러블 슈팅

## WebFlux → Spring MVC 전환 과정

### 문제

AI 챗봇은 WebFlux 기반, 실시간 채팅(WebSocket + STOMP) / 알림(SSE) / Spring Security는 서블릿 기반으로 설계되어 충돌이 발생했다.

### 원인

LangChain4j는 Blocking I/O 기반이므로, WebFlux에서 사용하려면 `Schedulers.boundedElastic()`으로 스레드를 분리하는 우회 처리가 필요했고, 이로 인해 WebFlux의 논블로킹 장점이 모두 상쇄됐다.
또한 WebSocket, SSE, Spring Security 등 나머지 모듈이 모두 서블릿 기반이어서 WebFlux와 구조적으로 충돌하는 상황이었다.

### 해결

Spring MVC + Java 21 Virtual Thread 로 전환

- 의존성 교체 - `spring-boot-starter-webflux` 제거, `spring-boot-starter-web` 추가
- SecurityConfig 변경 — `@EnableWebFluxSecurity` → `@EnableWebSecurity`, `SecurityWebFilterChain` → `SecurityFilterChain`
- SecurityUtils 단순화 — `ReactiveSecurityContextHolder` 기반의 비동기 체인 → `SecurityContextHolder` 기반의 동기 호출
- 컨트롤러 변경 — `Flux<ServerSentEvent>` → `SseEmitter` 기반으로 교체, LangChain4j `TokenStream`을 자연스럽게 사용

### 결과

전환을 통해 단순함과 성능을 모두 확보할 수 있었다. 코드 라인은 50% 감소했고 응답 시간은 52% 개선되었다. 
아키텍처 측면에서는 모든 모듈이 서블릿 기반으로 통일되어, 기존에 WebFlux와 충돌을 일으키던 WebSocket, SSE, Spring Security가 하나의 일관된 구조 안에서 자연스럽게 동작하게 되었다.

<br>

---

## Context Recall 0.9 기반 검색 품질 문제

### 문제

RAGAS 평가 중 Context Recall이 0.9로 측정되어, 정책 문서에 명확히 존재하는 정보임에도 검색하지 못하는 누락이 발생했다.

### 원인

Semantic Chunking은 인접한 텍스트 간의 임베딩 유사도를 기준으로 청크를 병합한다. 
원본 문서에서 섹션 4(교환 신청 방법)의 마지막 문장 "반품 상품을 포장하여 택배로 발송합니다"와 섹션 5(교환 처리 기간)의 첫 문장 "교환 상품이 도착한 후..."는 둘 다 배송/도착 맥락을 가지고 있어 임베딩 유사도가 0.75로 측정됐다. 
이 값이 SIMILARITY_THRESHOLD인 0.7을 초과하면서 두 섹션이 하나의 청크로 병합됐고, 그 결과 "교환 처리 기간"이라는 섹션 제목이 사라져 검색 시 매칭되지 않았다.

### 해결

임계값 조정이나 코드 레벨 강제 분리보다 문서 구조 자체를 명확히 하는 방향을 선택했다. 
섹션 4의 "반품 상품"을 "교환 상품"으로 수정해 섹션 5와의 의미적 연결을 끊었고, 섹션 5에는 처리 완료 후 액션까지 포함한 내용을 보강해 독립적인 청크로 분리되도록 했다. 
DB 재인제스천 후 "교환 처리 기간" 청크가 독립적으로 생성된 것을 확인했다.

### 결과

Context Recall이 0.90에서 1.00으로 개선되었고, 누락 답변이 0건으로 해소되었다. 평균 RAGAS 점수도 0.76에서 0.94로 24% 향상되었다. 
코드가 아닌 입력 데이터의 품질을 개선하는 것이 더 근본적인 해결책이 될 수 있다는 점을 확인한 사례였다.

<br>

---

## MessageListenerAdapter 동작 오류 문제

### 문제

`MessageListenerAdapter`의 동작 방식과 메서드 시그니처 불일치로 `redisMessageListenerContainer` 빈 생성에 실패했다.

### 원인

`MessageListenerAdapter`가 기대하는 파라미터 타입과 실제 구현체의 시그니처가 달라 메서드를 찾지 못했다.

- `MessageListenerAdapter`는 메시지 컨버터를 통해 변환된 데이터(`String` 또는 `byte[]`)를 넘겨주는 방식으로 동작한다.
- 반면 `RedisSubscriber.onMessage`의 파라미터는 `Message`, `byte[]` 형태로, `MessageListenerAdapter`가 해당 메서드를 찾지 못해 빈 생성 단계에서 오류가 발생했다.
- `Message` 객체 자체를 받으려면 `RedisSubscriber`가 `MessageListener` 인터페이스를 직접 구현하거나 파라미터 타입을 맞춰야 한다.

### 해결

`MessageListenerAdapter`를 제거하고 `RedisSubscriber`를 컨테이너에 직접 등록하는 방식으로 변경했다.

- `MessageListenerAdapter` 빈을 제거했다.
- `RedisSubscriber`를 `RedisMessageListenerContainer`에 직접 주입했다.
- `container.addMessageListener(redisSubscriber::onMessage, ...)` 형태로 등록해 어댑터를 거치지 않고 onMessage를 직접 호출하도록 수정했다.

### 결과

빈 생성 오류가 해소되었고, `chat.room.*`, `chat.read.*` 패턴의 메시지를 정상적으로 수신할 수 있게 되었다.

<br>

---

## STOMP Subscribe 프레임에서 accessor.getUser() 가 null 이 되는 문제

### 문제

`CONNECT` 프레임에서는 `accessor.getUser()`가 정상적으로 존재했지만, 이후 `SUBSCRIBE` 프레임에서는 `null`로 확인되어 `convertAndSendToUser()`가 메시지를 전달할 세션을 식별하지 못할 가능성이 있었다.

### 원인

`CONNECT`와 `SUBSCRIBE`의 `sessionId`가 동일함을 확인해 재연결 문제는 아니었고, 같은 세션 안에서 `Principal`만 유지되지 않는 것이 문제였다.

- 기존 코드는 `StompHeaderAccessor.wrap(message)` 방식으로 accessor를 가져오고 있었다.
- 이 방식은 메시지에 실제로 연결된 accessor를 가져오는 것이 아니라 새 wrapper를 생성하기 때문에, `Principal`과 같은 session-bound header 상태가 이후 처리 흐름에 유지되지 않았다.

### 해결

`StompHeaderAccessor.wrap()` 대신 `MessageHeaderAccessor.getAccessor()`를 사용해 메시지에 이미 연결된 accessor를 직접 가져오도록 수정했다.

- `StompHeaderAccessor.wrap(message)` → `MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)` 로 변경했다.
- `accessor.setLeaveMutable(true)`를 추가하고, 변경된 헤더를 포함한 새 메시지를 반환하도록 수정했다.
- `UserPrincipal`의 `getName()`이 `convertAndSendToUser()`에서 사용하는 userId 문자열과 일치하도록 구현했다.

### 결과

`SUBSCRIBE` 프레임에서 사용자 정보가 정상적으로 유지되었고, 실시간 채팅 세션에 인증된 사용자 정보가 정상적으로 연결되어 특정 사용자에게 채팅 메시지를 전송할 수 있게 되었다.

<br>

---

## 와일드카드 패턴 검색에서 인덱스를 사용할 수 없는 구조적 문제

### 문제

하이브리드 검색의 키워드 검색에서 `ILIKE '%keyword%'` 패턴을 사용하고 있었다. 
선행 와일드카드로 인해 B-Tree 인덱스를 사용할 수 없어 전체 테이블 스캔이 발생하는 구조였고, 데이터가 증가할수록 응답 시간이 선형적으로 저하될 위험이 있었다.

### 원인

B-Tree 인덱스는 접두사 검색(`LIKE 'abc%'`)에만 효과적이고, `LIKE '%abc%'`와 같은 선행 와일드카드 패턴에서는 인덱스를 사용할 수 없다. 
`EXPLAIN ANALYZE`결과에서`Seq Scan`이 발생하는 것을 확인했고, 현재는 데이터가 10개로 적어 차이가 없지만 데이터가 100만 건으로 증가하면 응답 시간이 5초 이상으로 늘어나는 구조적 문제였다.

### 해결

`pg_trgm` 확장과 GIN 인덱스를 도입했다. `pg_trgm`은 텍스트를 트리그램(3글자) 단위로 분해해 부분 매칭 검색을 최적화하고, GIN 인덱스는 역색인 구조로 `LIKE '%abc%'` 패턴 검색을 가속한다. 
변경은 Flyway 마이그레이션(V17__add_trigram_index.sql)으로 버전 관리하여 적용했다.

- `CREATE EXTENSION IF NOT EXISTS pg_trgm`으로 확장을 설치했다.
- `USING gin(text gin_trgm_ops)`으로 GIN 인덱스를 생성했다.
- `ANALYZE`로 통계를 갱신한 뒤 `EXPLAIN ANALYZE`로 인덱스 사용 여부를 검증했다.

### 결과

데이터 규모가 커질수록 Seq Scan 대비 최대 100배의 성능 개선이 기대된다. 
현재 데이터 규모에서는 차이가 없지만 데이터 증가에 강한 구조를 확보했고, Flyway를 통한 표준 절차로 DB 변경을 관리해 팀 협업과 버전 관리 측면에서도 안정성을 높였다.