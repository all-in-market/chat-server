import requests

TOKEN = "토큰 입력 필요"

response = requests.post(
    'https://hyu1335.cloud/chat/stream',
    headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {TOKEN}'
    },
    json={'message': '교환 정책 알려줘'},
    stream=True,
    timeout=(5, 60)
)
response.raise_for_status()

for line in response.iter_lines():
    if line:
        print(line.decode('utf-8').replace('data:', ''), end='', flush=True)
print()