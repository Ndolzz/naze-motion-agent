# Precision Studio Design System

Dark first professional tooling aesthetic. Precise, calm, information dense.

## Tokens (app/src/main/kotlin/com/naze/motion/app/ui/theme/NazeTheme.kt)
- NazeColors: background 0B0D10, surface 101318, elevated 151A20, strong 1A2027.
  Border 252B33, strong 303741. Text primary F2F4F7, secondary A6ADB7,
  muted 6F7782, disabled 4A515B. Primary accent 4C8DFF used sparingly.
  Success 35C98A, warning F2B84B, error F06464.
- NazeTypography: display 28, page title 23, section 17, body 14,
  caption 12, technical 12 monospace. Two weights only: 400 and 600.
- NazeSpacing: 4 based scale, default content padding 16, section spacing 24.
- NazeShapes: small 6, button 8, card 10, container 14.
- NazeAnimations: 150 to 250 ms. Subtle only.

## Components (ui/components)
NazeButton, NazeIconButton, NazeCard, NazeSection, NazeTextField,
NazeStatusLabel, NazeStatusDot, NazeDivider, NazeEmptyState,
NazeActionTimeline, NazeActionRow, NazeLog, NazeProgress.

## UI state model (ui/model/AgentUiState.kt)
Sealed AgentUiState: Idle, Planning, Validating, Executing, Recovering,
Completed, Failed(reason), Cancelled. No overlapping booleans.

## Rules
- Status always icon plus text plus color. Never color alone.
- Emoji are never used as UI icons.
- Stop Agent is always reachable during execution.
- Errors always show the reason and the recovery state.
- No gradient, glow, or heavy animation anywhere.
