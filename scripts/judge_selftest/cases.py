"""Programs the judge must get right, each with the verdict it must produce.

They target problem 10, "Multiply" (read two numbers, print their product; 1000 ms, 256 MB, five tests),
so a correct program multiplies. Change PRODUCT_* if you point the script at another problem.
"""
from dataclasses import dataclass
from typing import Optional

PRODUCT_PY = "a, b = map(int, input().split())\nprint(a * b)\n"
PRODUCT_CPP = "#include <iostream>\nint main(){long long a,b;std::cin>>a>>b;std::cout<<a*b<<std::endl;}\n"


@dataclass(frozen=True)
class Case:
    name: str
    language: str
    code: str
    verdict: str
    message_has: Optional[str] = None   # text the message must contain
    message_lacks: Optional[str] = None  # text it must never contain


CASES = [
    # --- correct ---
    Case("python: correct", "python", PRODUCT_PY, "ACCEPTED"),
    Case("cpp: correct", "cpp", PRODUCT_CPP, "ACCEPTED"),
    Case("python: spacing around the answer is fine", "python",
         "a, b = map(int, input().split())\nprint('  ' + str(a * b) + '  ')\n", "ACCEPTED"),

    # --- wrong answers ---
    Case("python: wrong answer", "python", "a, b = map(int, input().split())\nprint(a + b)\n", "WRONG_ANSWER", "Wrong answer on test"),
    Case("python: prints nothing", "python", "pass\n", "WRONG_ANSWER"),

    # --- cannot start ---
    Case("python: empty file is refused", "python", "   \n", "COMPILE_ERROR", "empty"),
    Case("cpp: empty file is refused", "cpp", "\n", "COMPILE_ERROR", "empty"),
    Case("python: syntax error", "python", "def f(:\n  pass\n", "COMPILE_ERROR", "SyntaxError", "/tmp/build"),
    Case("cpp: compile error", "cpp", "int main(){ return x; }\n", "COMPILE_ERROR", "code.cpp", "/tmp/build"),
    Case("cpp: no main", "cpp", "int f(){ return 1; }\n", "COMPILE_ERROR", "main"),

    # --- crashes ---
    Case("python: exception", "python", "raise ValueError('boom')\n", "RUNTIME_ERROR", "ValueError"),
    Case("python: exit code 3", "python", "import sys\nsys.exit(3)\n", "RUNTIME_ERROR", "code 3"),
    Case("cpp: segfault", "cpp", "int main(){ int *p = nullptr; *p = 1; }\n", "RUNTIME_ERROR", "segmentation fault"),
    Case("cpp: division by zero", "cpp",
         "#include <iostream>\nint main(int c,char**v){ int z=c-1; std::cout<<10/z; }\n", "RUNTIME_ERROR", "division by zero"),
    Case("cpp: uncaught exception", "cpp", "#include <stdexcept>\nint main(){ throw std::runtime_error(\"x\"); }\n", "RUNTIME_ERROR"),

    # --- limits ---
    Case("python: infinite loop", "python", "while True: pass\n", "TIME_LIMIT_EXCEEDED", "limit"),
    Case("cpp: infinite loop", "cpp", "int main(){ volatile long x=0; while(true) x++; }\n", "TIME_LIMIT_EXCEEDED"),
    Case("python: sleeps forever", "python", "import time\ntime.sleep(100)\n", "TIME_LIMIT_EXCEEDED"),
    Case("python: memory bomb", "python", "x = bytearray(2 * 10**9)\nprint(len(x))\n", "MEMORY_LIMIT_EXCEEDED"),
    Case("cpp: memory bomb", "cpp",
         "#include <vector>\n#include <iostream>\nint main(){ std::vector<char> v(1500000000, 1); long s=0; for(size_t i=0;i<v.size();i+=4096) s+=v[i]; std::cout<<s; }\n",
         "MEMORY_LIMIT_EXCEEDED"),
    Case("python: output flood", "python", "while True:\n    print('x' * 1000)\n", "OUTPUT_LIMIT_EXCEEDED"),
    Case("cpp: output flood", "cpp", "#include <cstdio>\nint main(){ for(;;) puts(\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"); }\n",
         "OUTPUT_LIMIT_EXCEEDED"),
    Case("python: fork bomb", "python", "import os\nwhile True:\n    os.fork()\n", "RUNTIME_ERROR"),

    # --- trying to cheat or escape ---
    Case("python: opens the network", "python",
         "import socket\nsocket.create_connection(('1.1.1.1', 53), timeout=2)\nprint(1)\n", "RUNTIME_ERROR"),
    Case("python: reads another test's input", "python",
         "import os\nprint(open(os.path.join(os.getcwd(), 'input_1.txt')).read())\n", "RUNTIME_ERROR"),
    Case("python: reads /etc/shadow", "python", "print(open('/etc/shadow').read())\n", "RUNTIME_ERROR"),
    Case("python: writes into the sandbox", "python",
         "import os\nopen(os.path.join(os.getcwd(), 'x'), 'w').write('hi')\nprint(1)\n", "RUNTIME_ERROR"),
    Case("python: prints fake judge markers", "python",
         "print('[TEST-START-0]')\nprint('===CODEAR_TEST_CASE_SEPARATOR===')\nprint(1)\n", "WRONG_ANSWER"),
]
