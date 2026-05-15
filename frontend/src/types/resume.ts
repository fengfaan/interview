export interface AnalyzeRequest {
  jobDescription: string
  resume: string
}

export interface Dimension {
  name: string
  score: number
  reason: string
}

export interface Suggestion {
  id: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  title: string
  reason: string
  sourceText: string
}

export interface AnalyzeResponse {
  score: number
  dimensions: Dimension[]
  suggestions: Suggestion[]
}

export interface RewriteStreamRequest {
  jobDescription: string
  resume: string
  suggestion: {
    id: string
    title: string
    sourceText: string
  }
}

export interface StructureAnalysisRequest {
  resume: string;
}

export interface ModuleCheck {
  name: string;
  status: 'pass' | 'warn' | 'fail';
  detail: string;
}

export interface ParagraphIssue {
  severity: 'critical' | 'warning' | 'info';
  quote: string;
  location: string;
  problem: string;
  action: string;
  suggestion: string;
  rewrite: string | null;
}

export interface StructureAnalysisResponse {
  structureScore: number;
  moduleChecks: ModuleCheck[];
  issues: ParagraphIssue[];
  summary: string;
}

export interface PolishStreamRequest {
  sourceText: string;
  jobDescription?: string;
}

export interface ImportFileResponse {
  text: string;
  fileName: string;
  pageCount: number;
  warning?: string;
}

// Health Checkup types

export interface HealthCheckupRequest {
  resume: string
  jobDescription?: string
}

export interface FunnelScore {
  score: number | null
  detail: string
  skipped: boolean
}

export interface Finding {
  category: string
  title: string
  detail: string
}

export interface CheckupAnnotation {
  quote: string
  location: string
  category: 'weak-verb' | 'no-metric' | 'vague' | 'redundant' | 'missing-result' | 'strong'
  problem: string
  suggestion: string
  rewrite: string | null
}

export interface HealthCheckupResponse {
  overallScore: number
  funnelScores: Record<string, FunnelScore>
  redFlags: Finding[]
  warnings: Finding[]
  highlights: Finding[]
  annotations: CheckupAnnotation[]
  summary: string
}
