import { useState } from "react";
import  Label  from "@/atoms/Label";
import Input  from "@/atoms/Input";
import Button  from "@/atoms/Button";
import Badge from "@/atoms/Badge";
import {Plus, X, ChevronsUpDown, Check} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
} from "@/components/ui/popover";
import {
  Command,
  CommandInput,
  CommandList,
  CommandItem,
  CommandEmpty,
} from "@/components/ui/command";

// Hardcoded DSA tags
const DSA_TAGS = [
  "Array",
  "String",
  "Hash Table",
  "Dynamic Programming",
  "Tree",
  "Graph",
  "Linked List",
  "Stack",
  "Queue",
  "Binary Search",
  "Recursion",
  "Two Pointers",
  "Sliding Window",
  "Greedy",
  "Backtracking",
  "Heap / Priority Queue",
  "Math",
  "Bit Manipulation",
  "Prefix Sum",
  "Sorting",
  "Depth First Search",
  "Breadth First Search",
  "Union Find",
  "Trie",
  "Matrix",
];

export default function TagsSelector({ tags, setTags }: any) {
  const [currentTag, setCurrentTag] = useState("");
  const [open, setOpen] = useState(false);

  const addTag = (tagToAdd?: string) => {
    const tag = (tagToAdd || currentTag).trim();
    if (tag && DSA_TAGS.includes(tag) && !tags.includes(tag)) {
      setTags([...tags, tag]);
    }
    setCurrentTag("");
    setOpen(false);
  };

  const removeTag = (tag: string) => {
    setTags(tags.filter((t) => t !== tag));
  };

  return (
    <div>
      <Label htmlFor="tags">Tags</Label>
      <div className="flex gap-2">

        <Popover open={open} onOpenChange={setOpen}>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              className="w-[160px] justify-between"
              onClick={() => setOpen(true)}
            >
              Select Tag
              <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
            </Button>
          </PopoverTrigger>
          <PopoverContent
            align="start"
            className="w-[220px] p-0 max-h-[250px] overflow-y-auto"
          >
            <Command>
              <CommandInput placeholder="Search tags..." />
              <CommandList>
                <CommandEmpty>No matching tag found.</CommandEmpty>
                {DSA_TAGS.filter((t) => !tags.includes(t)).map((tag) => (
                  <CommandItem
                    key={tag}
                    onSelect={() => addTag(tag)}
                    className="cursor-pointer"
                  >
                    <Check
                      className={cn(
                        "mr-2 h-4 w-4",
                        tags.includes(tag) ? "opacity-100" : "opacity-0"
                      )}
                    />
                    {tag}
                  </CommandItem>
                ))}
              </CommandList>
            </Command>
          </PopoverContent>
        </Popover>
      </div>

      {tags.length > 0 && (
        <div className="flex flex-wrap gap-2 mt-2">
          {tags.map((tag) => (
            <Badge
              key={tag}
              variant="default"
              className="cursor-pointer"
              onClick={() => removeTag(tag)}
            >
              {tag}
              <X className="w-3 h-3 ml-1" />
            </Badge>
          ))}
        </div>
      )}
    </div>
  );
}
