import { ChevronLeft, ChevronRight } from "lucide-react";
import Button from "@/atoms/Button";
import { cn } from "@/lib/utils";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

const Pagination = ({ currentPage, totalPages, onPageChange }: PaginationProps) => {
  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);
  
  const visiblePages = pages.filter((page) => {
    if (totalPages <= 7) return true;
    if (page === 1 || page === totalPages) return true;
    if (Math.abs(page - currentPage) <= 1) return true;
    return false;
  });

  const getVisiblePages = () => {
    if (totalPages <= 7) {
      return pages;
    }

    const pagesWithEllipsis: (number | string)[] = [];
    let lastPage = 0;

    for (const page of visiblePages) {
      if (lastPage !== 0 && page - lastPage > 1) {
        pagesWithEllipsis.push('...');
      }
      pagesWithEllipsis.push(page);
      lastPage = page;
    }
    return pagesWithEllipsis;
  }

  const pagesToRender = getVisiblePages();

  return (
    <div className="flex items-center justify-center gap-2">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
      >
        <ChevronLeft className="w-4 h-4" />
      </Button>

      {pagesToRender.map((page, index) =>
        typeof page === 'number' ? (
          <Button
            key={page}
            variant={currentPage === page ? "primary" : "outline"}
            size="sm"
            onClick={() => onPageChange(page)}
            className={cn(currentPage === page && "pointer-events-none")}
          >
            {page}
          </Button>
        ) : (
          <span key={`ellipsis-${index}`} className="px-2 text-muted-foreground">
            ...
          </span>
        )
      )}

      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
      >
        <ChevronRight className="w-4 h-4" />
      </Button>
    </div>
  );
};

export default Pagination;
