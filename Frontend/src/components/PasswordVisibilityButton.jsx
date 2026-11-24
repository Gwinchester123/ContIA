export default function PasswordVisibilityButton({
  visible,
  onToggle,
  label = visible ? "Ocultar senha" : "Mostrar senha",
  className = "password-visibility",
}) {
  return (
    <button
      type="button"
      className={className}
      onClick={onToggle}
      aria-label={label}
      aria-pressed={visible}
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
        {!visible && <path d="m4 4 16 16" />}
      </svg>
    </button>
  );
}
