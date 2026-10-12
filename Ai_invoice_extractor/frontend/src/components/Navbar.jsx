import { API_BASE_URL } from '../api/invoiceApi';

function Navbar({ onFilesSelected }) {
  function handleChange(event) {
    const files = Array.from(event.target.files);
    onFilesSelected(files);
    event.target.value = '';
  }

return (
  <nav className="navbar">
    <span className="navbar-brand">AI Invoice Extractor</span>
    <div className="navbar-actions">
      <label className="btn btn-outline">
        Upload
        <input type="file" accept="application/pdf" multiple hidden onChange={handleChange} />
      </label>
      <a className="btn btn-primary" href={`${API_BASE_URL}/invoices/export/csv`}>
        Download CSV (All)
      </a>
    </div>
  </nav>
);
}

export default Navbar;