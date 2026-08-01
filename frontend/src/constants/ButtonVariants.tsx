export const getButtonClasses = (variant) => {
  const base = "flex items-center justify-center h-9 px-3 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500";
  
  const variants = {
    secondary: "bg-gray-200 text-gray-900 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-100 dark:hover:bg-gray-600",
    accent: "bg-green-600 text-white hover:bg-blue-700",
  };
  
  return `${base} ${variants[variant] || variants.secondary}`;
};