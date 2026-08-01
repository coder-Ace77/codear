import { useState, useEffect } from "react";
import apiClient from "@/lib/apiClient";
import { AlertTriangle, WifiOff } from "lucide-react";
import Button from "@/atoms/Button";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter,
} from "@/components/ui/dialog";

const BackendHealthCheck = () => {
    const [isBlocked, setIsBlocked] = useState(false);
    const [open, setOpen] = useState(false);

    useEffect(() => {
        const checkHealth = async () => {
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 10000); // 1s timeout

                await apiClient.get("/user/health", {
                    signal: controller.signal,
                });

                clearTimeout(timeoutId);
                // Connection successful
            } catch (error) {
                // If error (timeout or network error)
                setIsBlocked(true);
                setOpen(true);
                console.error("Backend health check failed:", error);
            }
        };

        checkHealth();
    }, []);

    const handleClose = () => {
        setOpen(false);
    };

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogContent className="sm:max-w-md border-destructive/50 bg-destructive/10">
                <DialogHeader>
                    <div className="flex items-center gap-2 text-destructive">
                        <WifiOff className="h-6 w-6" />
                        <DialogTitle>Connection Warning</DialogTitle>
                    </div>
                    <DialogDescription className="pt-2 text-foreground font-medium">
                        Backend might be blocked or unreachable.
                    </DialogDescription>
                </DialogHeader>

                <div className="flex flex-col gap-4 py-2">
                    <div className="flex items-start gap-3 text-sm text-muted-foreground p-3 bg-background/50 rounded-md border">
                        <AlertTriangle className="h-5 w-5 text-yellow-500 shrink-0 mt-0.5" />
                        <p>
                            If you are on a <strong>company network or VPN</strong>, the firewall is likely blocking the connection to our servers.
                        </p>
                    </div>
                    <p className="text-sm">
                        Please switch to a <strong>personal network</strong> or mobile data for the best experience.
                    </p>
                </div>

                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="destructive" onClick={handleClose}>
                        I Understand
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default BackendHealthCheck;
