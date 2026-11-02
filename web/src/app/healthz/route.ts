/** ALB·컨테이너 헬스체크용. Next 서버가 요청을 받을 수 있으면 200 (백엔드 상태와는 무관) */
export function GET() {
  return new Response("ok", { headers: { "Cache-Control": "no-store" } });
}
