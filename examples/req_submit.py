import requests
import json
from datetime import datetime

# URL for the Spring Boot endpoint
# (Assuming your server is running on localhost:8080)
url = 'http://localhost:8082/submit'

# The payload (body) of the request
payload = {
    "userId": 123,
    "code": """n = int(input())
arr = list(map(int, input().split()))

# two sum logic
target = int(input())
num_map = {}
for i, num in enumerate(arr):
    complement = target - num
    if complement in num_map:
        print(num_map[complement], i)
        break
    num_map[num] = i    
""",
    "language": "python",
    "problemId": 4,
    "submittedAt": datetime.now().isoformat()
}

try:
    # Send the POST request
    # The `json=` argument automatically serializes the dict to JSON
    # and sets the 'Content-Type: application/json' header.
    response = requests.post(url, json=payload)

    # Raise an exception for bad status codes (4xx or 5xx)
    response.raise_for_status() 
    
    print(f"Server response: {response.text}")

except requests.exceptions.ConnectionError:
    print(f"Error: Connection refused. Is the server running at {url}?")
except requests.exceptions.RequestException as e:
    print(f"An error occurred: {e}")