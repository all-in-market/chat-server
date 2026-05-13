import requests

TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwicm9sZSI6IkJVWUVSIiwiZXhwIjoxNzc4Njc5NTE4LCJpYXQiOjE3Nzg2NzU5MTh9.nXN_VbzgDKgmFKHF562PPhou2onsK0NeTtT-Nh2U4Vg"

response = requests.post(
    'https://hyu1335.cloud/chat/stream',
    headers={
    'Content-Type': 'application/json',
    'Authorization': f'Bearer {TOKEN}'
    },
    json={'message': '교환 정책 알려줘'},
    stream=True
)

for line in response.iter_lines():
    if line:
        print(line.decode('utf-8').replace('data:', ''), end='', flush=True)
print()