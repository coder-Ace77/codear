export interface User {
  id?: number;
  username: string;
  name: string;
  email: string;
  password?: string;
  role: string;
  dailyStreak?: number;
  problemSolvedEasy?: number;
  problemSolvedMedium?: number;
  problemSolvedHard?: number;
  problemSolvedTotal?: number;
}
