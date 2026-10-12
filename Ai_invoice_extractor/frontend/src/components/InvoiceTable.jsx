const STATUS_LABELS = {
  PENDING_REVIEW: 'Pending review',
  CONFIRMED: 'Confirmed',
};

function InvoiceTable({ invoices, onConfirm, confirmingId }) {
  return (
    <section className="card">
      <div className="card-header">
        <h2>Invoices</h2>
        <span className="count">{invoices.length} total</span>
      </div>

      {invoices.length === 0 ? (
        <p className="empty">No invoices saved yet.</p>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Vendor</th>
                <th>Invoice No.</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map((invoice) => (
                <tr key={invoice.id}>
                  <td className="vendor">{invoice.vendorName}</td>
                  <td className="mono">{invoice.invoiceNumber}</td>
                  <td>
                    <span className={`badge badge-${invoice.status.toLowerCase()}`}>
                      {STATUS_LABELS[invoice.status] || invoice.status}
                    </span>
                  </td>
                  <td>
  {invoice.status === 'PENDING_REVIEW' ? (
    <button
      className="btn btn-primary btn-small"
      disabled={confirmingId === invoice.id}
      onClick={() => onConfirm(invoice.id)}
    >
      {confirmingId === invoice.id ? 'Confirming...' : 'Confirm'}
    </button>
  ) : (
    <span className="empty">-</span>
  )}
</td> 
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default InvoiceTable;