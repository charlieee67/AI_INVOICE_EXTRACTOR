import { useState } from 'react';

function UploadHero({ onFilesSelected }) {
  const [dragging, setDragging] = useState(false);

  function handleDrop(event) {
    event.preventDefault();
    setDragging(false);
    const files = Array.from(event.dataTransfer.files).filter(
      (file) => file.type === 'application/pdf'
    );
    if (files.length > 0) onFilesSelected(files);
  }

  function handleChange(event) {
    onFilesSelected(Array.from(event.target.files));
    event.target.value = '';
  }

  return (
    <section
      className={`hero ${dragging ? 'hero-dragging' : ''}`}
      onDragOver={(event) => {
        event.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
    >
      <h1 className="hero-title">AI Invoice Extractor</h1>
      <p className="hero-subtitle">
        Upload invoice PDFs (Portable Document Format) and get clean, structured data in seconds.
      </p>
      <label className="hero-button">
        Select PDF files
        <input type="file" accept="application/pdf" multiple hidden onChange={handleChange} />
      </label>
      <p className="hero-hint">or drop PDFs here</p>
    </section>
  );
}

export default UploadHero;