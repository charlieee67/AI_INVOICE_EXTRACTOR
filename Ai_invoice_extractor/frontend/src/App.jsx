// import { useEffect, useState } from 'react';
import Navbar from './components/Navbar';
import UploadHero from './components/UploadHero';
import UploadList from './components/UploadList';
import InvoiceTable from './components/InvoiceTable';
import { uploadInvoice, getInvoices, confirmInvoice } from './api/invoiceApi';
import { useCallback, useEffect, useState } from 'react';
import Toast from './components/Toast';

function App() {
  const [uploads, setUploads] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [confirmingId, setConfirmingId] = useState(null);
  const [toast, setToast] = useState(null);

const closeToast = useCallback(() => setToast(null), []);

function showToast(message, type = 'error') {
  setToast({ message, type, id: Date.now() });
}

  async function loadInvoices() {
    try {
      setInvoices(await getInvoices());
    } catch {
      // keep the old list if loading fails
    }
  }

  useEffect(() => {
    loadInvoices();
  }, []);

  function updateUpload(id, changes) {
    setUploads((current) =>
      current.map((item) => (item.id === id ? { ...item, ...changes } : item))
    );
  }

  async function handleFilesSelected(files) {
    const newItems = files.map((file, index) => ({
      id: `${Date.now()}-${index}`,
      name: file.name,
      file,
      status: 'waiting',
      message: '',
    }));

    setUploads((current) => [...newItems, ...current]);

    for (const item of newItems) {
      updateUpload(item.id, { status: 'uploading' });
      try {
        await uploadInvoice(item.file);
        updateUpload(item.id, { status: 'done', message: 'Saved, waiting for review' });
      } catch (error) {
        const status = error.status === 409 ? 'duplicate' : 'error';
        updateUpload(item.id, { status, message: error.message });
      }
    }

    await loadInvoices();
  }

 async function handleConfirm(id) {
  if (!window.confirm('Check the details first. Confirm this invoice?')) return;
  setConfirmingId(id);
  try {
    await confirmInvoice(id);
    await loadInvoices();
    showToast('Invoice confirmed', 'success');
  } catch (error) {
    showToast(error.message, 'error');
  } finally {
    setConfirmingId(null);
  }
}

  return (
  <>
    <Navbar onFilesSelected={handleFilesSelected} />
    <UploadHero onFilesSelected={handleFilesSelected} />
    <main className="container">
      <UploadList uploads={uploads} />
      <InvoiceTable invoices={invoices} onConfirm={handleConfirm} confirmingId={confirmingId} />
      <Toast toast={toast} onClose={closeToast} />
    </main>
    <footer className="footer">© 2026 AI Invoice Extractor</footer>
  </>
);
}
 
export default App;