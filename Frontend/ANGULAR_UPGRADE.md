# Angular Frontend Upgrade Roadmap (v8 to v18)

This document provides a comprehensive, structured roadmap for upgrading the Joindesk frontend from Angular 8 to Angular 18. Due to the age of the codebase (Angular 8, released in 2019) and the number of breaking changes in the ecosystem, a direct one-step upgrade will fail. We recommend a **phased, incremental migration strategy**.

---

## Migration Strategy Overview

| Phase | Target Version | Primary Upgrades & Breaking Changes |
| :--- | :--- | :--- |
| **Phase 1** | Angular 9 & 10 | IVY Compiler, RxJS 6, TypeScript 3.9 |
| **Phase 2** | Angular 11 & 12 | Strict Mode, Deprecation of TSLint (migrate to ESLint), Webpack 5 |
| **Phase 3** | Angular 13, 14 & 15 | Strict Types, Ivy-only distribution, Angular Standalone Components |
| **Phase 4** | Angular 16, 17 & 18 | Vite & Esbuild, Hydration/SSR, Signals, Control Flow (`@if`, `@for`) |

---

## Step-by-Step Phased Upgrade Guide

### Phase 1: Angular 8 -> Angular 10 (The Ivy Phase)

This step introduces Angular's standard compilation engine, Ivy.

1. **Pre-requisites:**
   - Upgrade Node.js from standard legacy to Node.js v12 or v14 (Angular 9/10 baseline).
2. **Execute Angular CLI Upgrades:**
   ```bash
   npx @angular/cli@8 update @angular/core@9 @angular/cli@9
   ```
   *Note: This command runs migrations, updating lazy loading syntax to dynamic imports (`import()`) automatically.*
3. **Migrate Libraries:**
   - Upgrade `ng-zorro-antd` to `9.x` (`ng update ng-zorro-antd@9`).
   - Upgrade `@swimlane/ngx-graph` to a version compatible with Angular 9 (e.g., `^7.0.0`).
4. **Upgrade to Angular 10:**
   ```bash
   npx @angular/cli@9 update @angular/core@10 @angular/cli@10
   ```

---

### Phase 2: Angular 10 -> Angular 12 (Modern Tooling & Linting)

This phase removes TSLint (deprecated) and updates build configurations to Webpack 5.

1. **Execute CLI Upgrades:**
   ```bash
   npx @angular/cli@10 update @angular/core@11 @angular/cli@11
   npx @angular/cli@11 update @angular/core@12 @angular/cli@12
   ```
2. **Migrate from TSLint to ESLint:**
   - Install Angular ESLint schematics:
     ```bash
     ng add @angular-eslint/schematics
     ```
   - Convert `tslint.json` rules automatically to `.eslintrc.json`:
     ```bash
     ng g @angular-eslint/schematics:convert-tslint-to-eslint jd-frontend
     ```
   - Delete `tslint.json` and remove the `codelyzer` devDependency.
3. **Update Styling & Third-Party UI Kits:**
   - Upgrade Bootstrap 4 to Bootstrap 5 (`npm i bootstrap@5`). Adjust layout variables since Bootstrap 5 dropped jQuery.
   - Upgrade `ng-zorro-antd` to `12.x`.

---

### Phase 3: Angular 12 -> Angular 15 (Typed Forms & Standalone Architecture)

This phase introduces major modernizing features such as Standalone components (optional but recommended) and strictly-typed reactive forms.

1. **Upgrade Angular CLI and Core:**
   ```bash
   npx @angular/cli@12 update @angular/core@13 @angular/cli@13
   npx @angular/cli@13 update @angular/core@14 @angular/cli@14
   npx @angular/cli@14 update @angular/core@15 @angular/cli@15
   ```
2. **Handle Strictly-Typed Reactive Forms (Angular 14+):**
   - Angular 14 automatically replaces `FormGroup` and `FormControl` with untyped variants (`UntypedFormGroup`, `UntypedFormControl`) during migration to prevent compilation errors.
   - *Action Item:* Gradually refactor forms in `/src/app/` to use modern strictly-typed interfaces for improved type safety.
3. **Deprecate Protractor:**
   - Protractor is fully EOL. Remove `protractor.conf.js` and `e2e/`.
   - *Action Item:* Install and configure Playwright or Cypress for End-to-End (E2E) verification:
     ```bash
     npm init playwright@latest
     ```

---

### Phase 4: Angular 15 -> Angular 18 (Signals & Modern Compilation)

This phase transitions the application to Angular 18, bringing state management with Signals, Vite builds, and modern template control flow.

1. **Execute Final Upgrades:**
   ```bash
   npx @angular/cli@15 update @angular/core@16 @angular/cli@16
   npx @angular/cli@16 update @angular/core@17 @angular/cli@17
   npx @angular/cli@17 update @angular/core@18 @angular/cli@18
   ```
2. **Refactor Components to Standalone (Optional but highly recommended):**
   - Run the automatic migration to standalone components, which removes `NgModule` overhead:
     ```bash
     ng g @angular/core:control-flow
     ng g @angular/core:standalone
     ```
3. **Adopt Modern Control Flow (Angular 17+):**
   - Transition template directives like `*ngIf`, `*ngFor`, and `*ngSwitch` to the native block-based control flow (`@if`, `@for`, `@switch`).
4. **Upgrade WebSockets / STOMP Integration:**
   - Update `sockjs-client` and `@stomp/stompjs` (replaces old legacy `stompjs` dependency) to ensure smooth real-time updates and standard-compliant socket communication.

---

## Key Dependency Mappings

| Dependency | Old Version (Angular 8) | New Version (Angular 18) | Action Required |
| :--- | :--- | :--- | :--- |
| **TypeScript** | `~3.4.5` | `~5.4.2` | Automatically managed by `ng update` |
| **RxJS** | `^6.5.4` | `^7.8.1` | Run `npm install rxjs@7` (or `@8`) |
| **Zone.js** | `^0.9.1` | `~0.14.4` | Managed by CLI upgrade |
| **ng-zorro-antd** | `^8.5.2` | `^18.0.0` | Heavy migration required due to component API changes |
| **Bootstrap** | `4.3.1` | `5.3.3` | Style adjustments needed (removal of jQuery dependency) |
| **Linter** | `tslint ~5.17.0` | `@typescript-eslint` | Migrate via `@angular-eslint` |
| **E2E Testing** | `protractor ^7.0.0` | `playwright ^1.44.0` | Full E2E test suite replacement |
