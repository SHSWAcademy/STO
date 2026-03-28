import { X } from 'lucide-react';

export function Modal({ isOpen, onClose, title, children, maxWidth = 'max-w-sm' }) {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
      <div className={`bg-white rounded-2xl w-full ${maxWidth} overflow-hidden shadow-2xl`}>
        {title !== undefined && (
          <div className="p-6 border-b border-stone-200 flex items-center justify-between">
            <h3 className="text-lg font-black text-stone-800">{title}</h3>
            <button onClick={onClose} className="p-1 text-stone-400 hover:text-stone-800 transition-colors">
              <X size={20} />
            </button>
          </div>
        )}
        {children}
      </div>
    </div>
  );
}
