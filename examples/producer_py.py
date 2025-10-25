from kafka import KafkaProducer
import json
from datetime import datetime

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

topic = 'code-submit'
message = {
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

print(f"Sending JSON to topic: {topic}")
producer.send(topic, message)

producer.flush()
producer.close()

print("Message sent successfully!")