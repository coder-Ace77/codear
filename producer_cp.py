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
    "code": """#include <iostream>
#include <vector>
#include <unordered_map>

int main() {
    int n;
    std::cin >> n;

    std::vector<int> arr(n);
    for (int i = 0; i < n; ++i) {
        std::cin >> arr[i];
    }

    int target;
    std::cin >> target;

    std::unordered_map<int, int> num_map;
    for (int i = 0; i < n; ++i) {
        int num = arr[i];
        int complement = target - num;
        
        if (num_map.find(complement) != num_map.end()) {
            std::cout << num_map[complement] << " " << i << std::endl;
            break;
        }
        num_map[num] = i;
    }
    
    return 0;
}
""",
    "language": "cpp",
    "problemId": 4,
    "submittedAt": datetime.now().isoformat()
}

print(f"Sending JSON to topic: {topic}")
producer.send(topic, message)

producer.flush()
producer.close()

print("Message sent successfully!")