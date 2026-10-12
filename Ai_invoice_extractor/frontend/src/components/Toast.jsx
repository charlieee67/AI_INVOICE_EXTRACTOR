import { useEffect } from 'react';

function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(onClose, 100000);
    return () => clearTimeout(timer);
  }, [toast, onClose]);

  if (!toast) return null;

  return (
    <div className={`toast toast-${toast.type}`} role="alert">
      <span>{toast.message}</span>
      <button className="toast-close" onClick={onClose} aria-label="Close">
        x
      </button>
    </div>
  );
}

export default Toast;