const LABELS = {
  waiting: 'Waiting',
  uploading: 'Reading invoice...',
  done: 'Done',
  duplicate: 'Already exists',
  error: 'Failed',
};



function UploadList({ uploads }) {
  return (
    <section className="card">
      <h2>Uploads</h2>
      {uploads.length === 0 ? (
        <p className="empty">No invoices uploaded yet.</p>
      ) : (
        <ul className="upload-list">
          {uploads.map((item) => (
            <li key={item.id} className="upload-item">
              <div>
                <strong>{item.name}</strong>
                {item.message && <div className="upload-message">{item.message}</div>}
              </div>
              <span className={`badge badge-${item.status}`}>{LABELS[item.status]}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default UploadList;