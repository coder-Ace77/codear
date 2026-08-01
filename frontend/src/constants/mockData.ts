export interface Problem {
  id: number;
  title: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  tags: string[];
  description?: string;
  
}

export interface ProblemSummary{
  id: number;
  title:string;
  difficulty: 'Easy' | 'Medium' | 'Hard',
  tags: string[]
}

export interface User {
  name: string;
  username: string;
  email: string;
  avatar: string;
  joinDate: string;
  solved: {
    easy: number;
    medium: number;
    hard: number;
  };
  streak: number;
  rank: number;
}

export const mockProblems: Problem[] = [
  {
    id: 1,
    title: "Two Sum",
    difficulty: "Easy",
    tags: ["Array", "Hash Table"],
    description: "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target."
  },
  {
    id: 2,
    title: "Add Two Numbers",
    difficulty: "Medium",
    tags: ["Linked List", "Math", "Recursion"],
    description: "You are given two non-empty linked lists representing two non-negative integers."
  },
  {
    id: 3,
    title: "Longest Substring Without Repeating Characters",
    difficulty: "Medium",
    tags: ["String", "Hash Table", "Sliding Window"],
  },
  {
    id: 4,
    title: "Median of Two Sorted Arrays",
    difficulty: "Hard",
    tags: ["Array", "Binary Search", "Divide and Conquer"],
  },
  {
    id: 5,
    title: "Longest Palindromic Substring",
    difficulty: "Medium",
    tags: ["String", "Dynamic Programming"],
  },
  {
    id: 6,
    title: "ZigZag Conversion",
    difficulty: "Medium",
    tags: ["String"],
  },
  {
    id: 7,
    title: "Reverse Integer",
    difficulty: "Easy",
    tags: ["Math"],
  },
  {
    id: 8,
    title: "String to Integer (atoi)",
    difficulty: "Medium",
    tags: ["String"],
  },
  {
    id: 9,
    title: "Palindrome Number",
    difficulty: "Easy",
    tags: ["Math"],
  },
  {
    id: 10,
    title: "Regular Expression Matching",
    difficulty: "Hard",
    tags: ["String", "Dynamic Programming", "Recursion"],
  },
  {
    id: 11,
    title: "Container With Most Water",
    difficulty: "Medium",
    tags: ["Array", "Two Pointers", "Greedy"],
  },
  {
    id: 12,
    title: "Integer to Roman",
    difficulty: "Medium",
    tags: ["Hash Table", "Math", "String"],
  },
];

export const mockUser: User = {
  name: "Alex Johnson",
  username: "alexj_dev",
  email: "alex.johnson@example.com",
  avatar: "AJ",
  joinDate: "2023-06-15",
  solved: {
    easy: 47,
    medium: 32,
    hard: 8,
  },
  streak: 15,
  rank: 1247,
};

