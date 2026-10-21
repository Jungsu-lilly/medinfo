import Link from "next/link";
import { BackIcon } from "./Icons";

/** 뒤로 + 제목 + 오른쪽 요소. 상세 화면 공통 머리 */
export default function BackHeader({
  backHref,
  title,
  right,
  bordered = true,
}: {
  backHref: string;
  title?: string;
  right?: React.ReactNode;
  bordered?: boolean;
}) {
  return (
    <header
      style={{
        display: "flex",
        alignItems: "center",
        gap: 4,
        height: 52,
        padding: "0 8px",
        borderBottom: bordered ? "1px solid var(--line)" : undefined,
      }}
    >
      <Link
        href={backHref}
        aria-label="뒤로"
        style={{ display: "flex", alignItems: "center", justifyContent: "center", width: 44, height: 44 }}
      >
        <BackIcon size={22} />
      </Link>
      {title ? <h1 style={{ flex: 1, fontSize: 17, fontWeight: 700 }}>{title}</h1> : <span style={{ flex: 1 }} />}
      {right}
    </header>
  );
}
