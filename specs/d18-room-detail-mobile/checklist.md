# CHECKLIST D18

## Gobernanza
- [x] constitution.md / AGENTS.md / memoria.md leídas
- [x] Declaración SPEC/PLAN/TASK/ESTADO antes de tocar código
- [x] Spec, plan, task creados
- [x] Rama `feature/room-detail-mobile` desde `origin/main` @ `1ab64eb`
- [x] Sin mezclar con D16 (#32, ya mergeado en main)
- [x] COST-001 $0

## Contrato
- [x] Backend auditado en solo lectura (`SearchEndpoint.cs`, `SearchContracts.cs`, `SearchService.cs`, `MediaContracts.cs`)
- [x] `GET /api/v1/rooms/{id}` confirmado (auth required)
- [x] `/rooms/search` documentado para futura spec

## Implementación
- [x] RoomApi + DTOs
- [x] RoomRepository + errores tipados
- [x] Domain + GetRoomDetailUseCase
- [x] RoomDetailRoute/Screen/ViewModel
- [x] Smart Match click usa roomId
- [x] NO PropertyDetail desde Smart Match
- [x] Back y bottom nav preservados

## Tests 1–12
- [x] 1 GET room detail ×1
- [x] 2 200 → Content
- [x] 3 404 → NotFound
- [x] 4 401 → RequiresAuth
- [x] 5 429 tipado
- [x] 6 network tipado
- [x] 7 click usa roomId
- [x] 8 NO PropertyDetail
- [x] 9 NO /properties/{roomId}
- [x] 10 back
- [x] 11 DTO mapping
- [x] 12 screen rendering

## Gates y cierre
- [x] clean compileDebugKotlin
- [x] testDebugUnitTest
- [x] lintDebug
- [x] assembleDebug
- [x] git diff --check
- [ ] CI remoto verde (verificar tras push)
- [ ] PR separado, sin merge
- [x] memoria.md actualizada
