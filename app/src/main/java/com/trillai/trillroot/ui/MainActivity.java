package com.trillai.trillroot.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.trillai.trillroot.R;
import com.trillai.trillroot.core.Checklist;
import com.trillai.trillroot.core.DeviceInfo;
import com.trillai.trillroot.core.PropertySignals;
import com.trillai.trillroot.core.ReportFormatter;
import com.trillai.trillroot.core.RootAccess;
import com.trillai.trillroot.core.RootabilityAssessment;
import com.trillai.trillroot.core.Route;
import com.trillai.trillroot.core.RouteEngine;
import com.trillai.trillroot.device.CommandRunner;
import com.trillai.trillroot.device.DeviceCollector;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.trillai.trillroot.ui.Widgets.*;

/** Native, offline readiness assistant. It cannot and does not install root. */
public final class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 41;
    private static final String PREFS = "trillroot_preferences";
    private static final String CHECKED = "checked_preparation";
    private static final String TERMS_ACCEPTED = "terms_accepted_v1";

    private static final String TERMS =
            "TRILL ROOT TERMS OF USE AND SAFETY NOTICE\n\n" +
            "Effective November 3, 2026\n\n" +
            "1. PURPOSE. Trill Root is an offline educational and device-readiness assistant. It reads ordinary Android build information, explains documented bootloader and root-project workflows, and can optionally ask an already-installed su program to run the read-only id command. It does not install root, exploit Android, unlock a bootloader, bypass Factory Reset Protection, remove network or carrier locks, defeat KnoxGuard or mobile-device management, flash firmware, or guarantee compatibility.\n\n" +
            "2. AUTHORIZATION. Use this app only on a device you own or are explicitly authorized to inspect. You are responsible for complying with law, contracts, workplace or school policy, carrier terms, warranties, export rules, and the device maker's terms. Do not use it to access another person's device or evade ownership, account, payment, management, or anti-theft controls.\n\n" +
            "3. RISK. Bootloader unlocking and device modification can erase data, prevent boot, weaken security, break updates, payments, streaming, health or enterprise apps, trigger permanent warranty or hardware-security flags, expose private information, or require paid repair. A backup is not a guarantee of recovery. Never relock a bootloader while modified images are installed. Battery charge, exact model, region, carrier, firmware revision, rollback index, and matching stock recovery files all matter.\n\n" +
            "4. ACCURACY AND SCOPE. Device properties may be absent, stale, vendor-specific, spoofed, or contradictory. A readiness result is not an attestation, vulnerability assessment, legal opinion, compatibility certificate, or instruction to proceed. Unknown means unknown. Historical one-click tools and exploits may be obsolete, malicious, incompatible, or unsafe on patched devices; Trill Root does not bundle or execute them. Verify every material fact with current official documentation for the exact model and build.\n\n" +
            "5. PRIVACY. Trill Root requests no Android runtime permissions and has no Internet permission, account system, analytics, advertising, or in-app comment service. Device and checklist data stays on the device unless you deliberately export a report or open an external documentation link in another app. Review exported build fingerprints before sharing because they can reveal detailed software information.\n\n" +
            "6. THIRD-PARTY MATERIAL. Names such as Android, Samsung, Magisk, KernelSU, KingRoot, KingoRoot, Towelroot, Sunshine, and MediaTek belong to their respective owners. Mentioning a technology does not mean endorsement, affiliation, safety, availability, or inclusion. External pages and tools have their own terms and privacy practices. Trill Root does not provide firmware mirrors, combination firmware, credentials, copyrighted packages, or bypass services.\n\n" +
            "7. NO WARRANTY. TO THE MAXIMUM EXTENT PERMITTED BY LAW, THE APP AND ITS INFORMATION ARE PROVIDED AS IS AND AS AVAILABLE, WITHOUT WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, NON-INFRINGEMENT, ACCURACY, AVAILABILITY, OR RESULTS. NOTHING IN THESE TERMS EXCLUDES RIGHTS THAT CANNOT LAWFULLY BE EXCLUDED.\n\n" +
            "8. RESPONSIBILITY AND LIMITATION. You control whether to leave the app, download third-party software, unlock, patch, or flash. The app never makes those changes for you. To the maximum extent permitted by law, contributors are not liable for lost data, lost access, device damage, security compromise, business interruption, warranty impact, or indirect, special, incidental, exemplary, or consequential loss arising from use of the app or external material.\n\n" +
            "9. SAFE USE. Stop if the exact model cannot be confirmed, official unlock eligibility is absent, a device is managed or account-locked, firmware does not match, instructions conflict, or you cannot restore stock software. Do not treat a Try Anyway action, forum comment, database entry, AI response, or old exploit name as evidence of safety. Trill Root intentionally provides no such bypass action.\n\n" +
            "10. AGREEMENT. By checking the box below, you confirm that you read this entire notice, are authorized to inspect this device, understand that no rooting or bypass capability is included, and accept responsibility for decisions made outside the app. You may decline and exit. You can review these terms later from About.";

    private final AtomicInteger generation = new AtomicInteger();
    private final Handler main = new Handler(Looper.getMainLooper());
    private ExecutorService worker;
    private CommandRunner runner;
    private DeviceCollector collector;
    private SharedPreferences preferences;
    private LinearLayout root;
    private LinearLayout content;
    private ScrollView scroll;
    private final LinearLayout[] navigation = new LinearLayout[4];
    private final TextView[] navigationLabels = new TextView[4];
    private final ImageView[] navigationIcons = new ImageView[4];
    private DeviceInfo info;
    private RootAccess rootAccess = RootAccess.initial();
    private boolean loading;
    private boolean probing;
    private int tab;
    private String routeId;
    private String pendingReport;
    private AlertDialog activeDialog;
    private OnBackInvokedCallback backCallback;
    private boolean onboarding;
    private boolean reviewingTerms;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        worker = Executors.newFixedThreadPool(2);
        runner = new CommandRunner();
        collector = new DeviceCollector(runner);
        if (state != null) {
            tab = Math.max(0, Math.min(3, state.getInt("tab", 0)));
            routeId = state.getString("route");
            pendingReport = state.getString("pendingReport");
        }
        configureWindow();
        onboarding = !preferences.getBoolean(TERMS_ACCEPTED, false);
        if (onboarding) showIntro(0);
        else launchMainApp();
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::handleBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    private void launchMainApp() {
        onboarding = false;
        buildShell();
        render();
        refreshDevice();
    }

    private LinearLayout onboardingPage() {
        LinearLayout page = column(this);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(BG);
        int p = dp(this, 28);
        page.setPadding(p, p, p, p);
        return page;
    }

    private void showIntro(int index) {
        if (isFinishing() || isDestroyed()) return;
        LinearLayout page = onboardingPage();
        ImageView logo = icon(this, R.drawable.ic_launcher, index == 1 ? MAGENTA : ACCENT);
        int logoSize = dp(this, index == 2 ? 164 : 104);
        page.addView(logo, new LinearLayout.LayoutParams(logoSize, logoSize));
        String title = index == 0 ? "by Brian Cross"
                : index == 1 ? "a Trill Production innovation!" : "Trill Root";
        TextView heading = heading(this, title, index == 2 ? 38 : 30);
        heading.setGravity(Gravity.CENTER);
        add(page, heading, 28);
        if (index == 2) {
            TextView description = text(this,
                    "A transparent, offline assistant that identifies your Android build and evaluates documented root-readiness signals without exploits or silent changes.",
                    16, MUTED, false);
            description.setGravity(Gravity.CENTER);
            add(page, description, 18);
        }
        setContentView(page);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(550);
        fade.setRepeatMode(Animation.REVERSE);
        fade.setRepeatCount(1);
        page.startAnimation(fade);
        main.postDelayed(() -> {
            if (!onboarding) return;
            if (index < 2) showIntro(index + 1); else showTerms(false);
        }, index == 2 ? 1900 : 1350);
    }

    private void showTerms(boolean reviewOnly) {
        LinearLayout shell = column(this);
        shell.setBackgroundColor(BG);
        shell.setPadding(dp(this, 20), dp(this, 18), dp(this, 20), dp(this, 18));
        add(shell, heading(this, "Terms, safety & legal notice", 28), 0);
        add(shell, text(this, "Scroll to the end to unlock acceptance. Read carefully; this is not a root or bypass tool.", 14, WARNING, false), 8);
        ScrollView termsScroll = new ScrollView(this);
        TextView body = text(this, TERMS, 14, TEXT, false);
        body.setTextIsSelectable(true);
        body.setPadding(0, dp(this, 12), 0, dp(this, 20));
        termsScroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        shell.addView(termsScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (reviewOnly) {
            reviewingTerms = true;
            add(shell, button(this, "Close terms", true, v -> { reviewingTerms = false; buildShell(); render(); }), 12);
            setContentView(shell);
            return;
        }
        CheckBox agree = new CheckBox(this);
        agree.setText("I read and agree to the Terms of Use and safety notice");
        agree.setTextColor(TEXT);
        agree.setButtonTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{ACCENT, MUTED}));
        agree.setEnabled(false);
        Button proceed = button(this, "Continue", true, v -> showPermissionDisclosure());
        proceed.setEnabled(false);
        proceed.setAlpha(0.45f);
        agree.setOnCheckedChangeListener((v, checked) -> {
            proceed.setEnabled(checked);
            proceed.setAlpha(checked ? 1f : 0.45f);
        });
        termsScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            View child = termsScroll.getChildAt(0);
            if (child != null && y + termsScroll.getHeight() >= child.getHeight() - dp(this, 20)) {
                agree.setEnabled(true);
            }
        });
        add(shell, agree, 10);
        add(shell, proceed, 8);
        setContentView(shell);
    }

    private void showPermissionDisclosure() {
        LinearLayout page = onboardingPage();
        page.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        add(page, eyebrow(this, "PERMISSION DISCLOSURE"), 20);
        add(page, heading(this, "No runtime permissions needed.", 32), 14);
        add(page, text(this,
                "Trill Root reads public Android build fields and a short allowlist of system properties. It does not request contacts, phone, SMS, location, camera, microphone, storage, accessibility, device-admin, VPN, or Internet access.",
                16, TEXT, false), 20);
        notice(page, "Optional existing-root check",
                "Only after a separate confirmation, Trill Root may run su -c id. Your current root manager controls the prompt. The app does not retry, install root, or change files.", CYAN);
        notice(page, "External actions stay visible",
                "Documentation opens in your browser. Reports use Android's document picker and are written only where you choose.", ACCENT);
        add(page, button(this, "I understand — identify this device", true, v -> showIdentification(0)), 24);
        setContentView(page);
    }

    private void showIdentification(int step) {
        final String[] messages = {
                "Starting local, permission-free inspection…",
                "Reading Android version and API level…",
                "Identifying manufacturer, model, and codename…",
                "Reading build and security patch labels…",
                "Checking reported bootloader state…",
                "Checking OEM-unlock support signal…",
                "Reviewing verified-boot state…",
                "Reading slot and dynamic-partition signals…",
                "Comparing documented route requirements…",
                "Ready — no device changes were made."
        };
        LinearLayout page = onboardingPage();
        ImageView logo = icon(this, R.drawable.ic_shield, step % 2 == 0 ? CYAN : MAGENTA);
        page.addView(logo, new LinearLayout.LayoutParams(dp(this, 112), dp(this, 112)));
        TextView title = heading(this, "Identifying your device, please wait a moment!", 27);
        title.setGravity(Gravity.CENTER);
        add(page, title, 26);
        TextView status = text(this, messages[Math.min(step, messages.length - 1)], 15, ACCENT, true);
        status.setGravity(Gravity.CENTER);
        add(page, status, 18);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(messages.length);
        progress.setProgress(step + 1);
        progress.setProgressTintList(ColorStateList.valueOf(CYAN));
        add(page, progress, 22);
        setContentView(page);
        if (step + 1 < messages.length) {
            main.postDelayed(() -> showIdentification(step + 1), 280);
        } else {
            main.postDelayed(() -> {
                preferences.edit().putBoolean(TERMS_ACCEPTED, true).apply();
                launchMainApp();
            }, 650);
        }
    }

    private void configureWindow() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
    }

    private void buildShell() {
        root = column(this);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                root.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            }
            root.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });

        LinearLayout header = row(this);
        int p = dp(this, 20);
        header.setPadding(p, dp(this, 14), p, dp(this, 12));
        ImageView shield = icon(this, R.drawable.ic_shield, ACCENT);
        header.addView(shield, new LinearLayout.LayoutParams(dp(this, 36), dp(this, 36)));
        LinearLayout brand = column(this);
        LinearLayout.LayoutParams brandLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        brandLp.leftMargin = dp(this, 10);
        brand.addView(heading(this, "Trill Root", 23));
        brand.addView(text(this, "ROOT READINESS · OFFLINE · TRANSPARENT", 9, MUTED, true));
        header.addView(brand, brandLp);
        header.addView(pill(this, "OFFLINE", CYAN));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        content = row(this);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(this, 20), dp(this, 8), dp(this, 20), dp(this, 28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = row(this);
        nav.setBackgroundColor(BG);
        nav.setPadding(dp(this, 12), dp(this, 8), dp(this, 12), dp(this, 8));
        String[] labels = {"Overview", "Routes", "Checklist", "About"};
        int[] icons = {R.drawable.ic_overview, R.drawable.ic_routes, R.drawable.ic_checklist, R.drawable.ic_info};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout item = column(this);
            item.setGravity(Gravity.CENTER);
            item.setMinimumHeight(dp(this, 58));
            item.setFocusable(true);
            item.setContentDescription(labels[i]);
            ImageView image = icon(this, icons[i], MUTED);
            item.addView(image, new LinearLayout.LayoutParams(dp(this, 21), dp(this, 21)));
            TextView label = text(this, labels[i], 11, MUTED, true);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
            labelLp.topMargin = dp(this, 5);
            item.addView(label, labelLp);
            item.setOnClickListener(v -> {
                tab = index;
                routeId = null;
                render();
                scroll.smoothScrollTo(0, 0);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.leftMargin = dp(this, 3);
            lp.rightMargin = dp(this, 3);
            nav.addView(item, lp);
            navigation[i] = item;
            navigationLabels[i] = label;
            navigationIcons[i] = image;
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        root.requestApplyInsets();
    }

    private void render() {
        content.removeAllViews();
        MaxWidthLayout page = new MaxWidthLayout(this);
        content.addView(page, new LinearLayout.LayoutParams(-1, -2));
        for (int i = 0; i < navigation.length; i++) {
            boolean selected = tab == i;
            navigation[i].setBackground(rounded(this, selected ? 0xFF203B2B : BG, 16, 0));
            navigation[i].setSelected(selected);
            navigationLabels[i].setTextColor(selected ? ACCENT : MUTED);
            navigationIcons[i].setImageTintList(ColorStateList.valueOf(selected ? ACCENT : MUTED));
        }
        if (tab == 0) overview(page);
        else if (tab == 1) {
            Route selected = findRoute();
            if (selected == null) routes(page); else routeDetail(page, selected);
        } else if (tab == 2) checklist(page);
        else about(page);
        scroll.post(() -> scroll.scrollTo(0, 0));
    }

    private void overview(LinearLayout page) {
        add(page, eyebrow(this, "ROOT READINESS / 01"), 4);
        add(page, heading(this, "Know your device.\nChoose your route.", 32), 12);
        add(page, text(this, "A clear guide to supported root options. No exploit lottery. No silent changes.", 15, MUTED, false), 12);

        LinearLayout hero = card(this);
        hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF1D3B30, 0xFF14232C}));
        ((GradientDrawable) hero.getBackground()).setCornerRadius(dp(this, 22));
        ((GradientDrawable) hero.getBackground()).setStroke(dp(this, 1), 0xFF3D5D45);
        LinearLayout top = row(this);
        TextView label = eyebrow(this, "THIS DEVICE");
        top.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(pill(this, "READ-ONLY", ACCENT));
        add(hero, top, 0);
        add(hero, heading(this, info == null ? "Reading device…" : info.title(), 27), 18);
        add(hero, text(this, info == null ? "Using public build information and optional system properties."
                : info.manufacturer + "  ·  Android " + info.androidVersion + "  ·  API " + info.sdk,
                14, MUTED, false), 7);
        add(hero, text(this, rootAccess.label(), 18, rootAccess.isConfirmed() ? ACCENT : TEXT, true), 22);
        add(hero, text(this, rootAccess.detail, 13, MUTED, false), 7);
        Button rootButton = button(this, probing ? "Waiting for root manager…" : "Check existing root access", true,
                v -> confirmRootProbe());
        rootButton.setEnabled(!loading && !probing);
        if (loading || probing) rootButton.setAlpha(0.55f);
        add(hero, rootButton, 18);
        Button refresh = button(this, loading ? "Reading device information…" : "Refresh device information", false,
                v -> refreshDevice());
        refresh.setEnabled(!loading);
        add(hero, refresh, 9);
        add(hero, text(this, "The root check may show your root manager's prompt. It runs only “id” and never installs root.", 12, MUTED, false), 11);
        add(page, hero, 22);

        RootabilityAssessment assessment = RootabilityAssessment.evaluate(info, rootAccess);
        LinearLayout verdict = card(this);
        add(verdict, eyebrow(this, "AUTO-DETECTED READINESS VERDICT"), 0);
        add(verdict, heading(this, assessment.title, 23), 10);
        add(verdict, text(this, assessment.explanation, 14, MUTED, false), 9);
        int verdictColor = assessment.verdict == RootabilityAssessment.Verdict.NOT_OFFICIALLY_SUPPORTED
                ? WARNING : assessment.verdict == RootabilityAssessment.Verdict.UNKNOWN ? CYAN : ACCENT;
        add(verdict, pill(this, assessment.verdict.name().replace('_', ' '), verdictColor), 14);
        add(page, verdict, 16);

        LinearLayout next = card(this);
        add(next, eyebrow(this, "NEXT STEP TO EVALUATE"), 0);
        add(next, heading(this, RouteEngine.recommendation(info, rootAccess), 21), 10);
        add(next, text(this, "A route is a documented option, not a promise that this device can be rooted.", 14, MUTED, false), 9);
        add(next, button(this, "Compare documented routes", false, v -> switchTab(1)), 16);
        add(page, next, 16);

        LinearLayout device = card(this);
        add(device, heading(this, "Device snapshot", 21), 0);
        if (info == null) {
            add(device, text(this, loading ? "Collecting non-privileged information…" : "Refresh to read device information.", 14, MUTED, false), 14);
        } else {
            add(device, text(this, "Reported by Android; not hardware attestation.", 13, MUTED, false), 6);
            detail(device, "Model", info.model);
            detail(device, "Codename", info.device);
            detail(device, "Android / API", info.androidVersion + " / " + info.sdk);
            detail(device, "Security patch", info.securityPatch);
            detail(device, "Bootloader", PropertySignals.lockLabel(info));
            detail(device, "OEM unlock", PropertySignals.supportLabel(info));
            detail(device, "Verified boot", PropertySignals.verifiedBootLabel(info));
            detail(device, "Active slot", PropertySignals.slotLabel(info));
            detail(device, "Dynamic partitions", PropertySignals.dynamicLabel(info));
            int firstApi = PropertySignals.firstApi(info);
            detail(device, "Launch API", firstApi > 0 ? String.valueOf(firstApi) : "Not exposed");
            detail(device, "CPU ABI", info.abi);
            detail(device, "Build ID", info.displayId);
            add(device, text(this, "Build fingerprint", 13, MUTED, false), 12);
            TextView fingerprint = text(this, info.fingerprint, 12, CYAN, false);
            fingerprint.setTypeface(Typeface.MONOSPACE);
            fingerprint.setTextIsSelectable(true);
            add(device, fingerprint, 6);
            if (PropertySignals.isEmulatorHint(info)) {
                add(device, text(this, "Emulator-like build detected. Emulator results do not establish retail-device compatibility.", 13, WARNING, false), 12);
            }
        }
        add(page, device, 16);
        notice(page, "Unknown is not a verdict", "Missing or conflicting boot properties stay unconfirmed. Verify lock state in the manufacturer's bootloader screen before making changes.", WARNING);
        notice(page, "This app does not root your device", "Supported rooting normally requires OEM unlock eligibility, exact firmware, and a computer. TrillRoot does not bypass locks, exploit Android, or flash partitions.", CYAN);
    }

    private void routes(LinearLayout page) {
        add(page, eyebrow(this, "SUPPORTED PATHS / 02"), 4);
        add(page, heading(this, "Routes, not guesses.", 32), 12);
        add(page, text(this, "Compare legitimate options for a device you control. Nothing here runs automatically.", 15, MUTED, false), 12);
        notice(page, RouteEngine.recommendation(info, rootAccess),
                "Exact model, carrier policy, firmware, and kernel support determine whether a route is available.", CYAN);
        if (info != null && PropertySignals.unlockSupport(info.property(PropertySignals.OEM_SUPPORTED)) == PropertySignals.Signal.NO) {
            notice(page, "Official unlock may not be available", "This build reports no OEM-unlock support. Confirm with your manufacturer. Do not try to bypass carrier, account, or management restrictions.", WARNING);
        }
        for (Route route : RouteEngine.ordered(info, rootAccess)) {
            LinearLayout card = card(this);
            add(card, eyebrow(this, route.category), 0);
            add(card, heading(this, route.title, 22), 10);
            add(card, text(this, route.summary, 14, MUTED, false), 9);
            add(card, text(this, route.suitability, 13, CYAN, false), 12);
            add(card, button(this, "Review route", false, v -> {
                routeId = route.id;
                render();
            }), 16);
            add(page, card, 16);
        }
    }

    private void routeDetail(LinearLayout page, Route route) {
        Button back = button(this, "←  All routes", false, v -> { routeId = null; render(); });
        add(page, back, 0);
        add(page, eyebrow(this, route.category), 22);
        add(page, heading(this, route.title, 31), 10);
        add(page, text(this, route.summary, 15, MUTED, false), 12);
        notice(page, "Before choosing this route", route.suitability, CYAN);
        LinearLayout requirements = card(this);
        add(requirements, heading(this, "What you need", 21), 0);
        for (String value : route.requirements) bullet(requirements, value, "•", ACCENT);
        add(page, requirements, 16);
        LinearLayout steps = card(this);
        add(steps, heading(this, "The documented workflow", 21), 0);
        for (int i = 0; i < route.steps.size(); i++) bullet(steps, route.steps.get(i), String.format(java.util.Locale.US, "%02d", i + 1), CYAN);
        add(page, steps, 16);
        notice(page, "Stop-and-check warning", route.warning, WARNING);
        add(page, button(this, route.documentationLabel, true, v -> openUrl(route.documentation)), 18);
        if (route.id.equals("magisk") || route.id.equals("samsung") || route.id.equals("existing")) {
            add(page, button(this, "Official Magisk releases", false, v -> openUrl(RouteEngine.MAGISK_RELEASES)), 10);
        }
        LinearLayout command = card(this);
        add(command, heading(this, "Optional read-only PC command", 19), 0);
        add(command, text(this, "Run with Android platform-tools on your computer, not in this app. Copying does not execute anything.", 13, MUTED, false), 8);
        TextView code = text(this, route.readOnlyCommand, 14, ACCENT, false);
        code.setTypeface(Typeface.MONOSPACE);
        code.setTextIsSelectable(true);
        add(command, code, 14);
        add(command, button(this, "Copy command", false, v -> copyCommand(route.readOnlyCommand)), 14);
        add(page, command, 16);
        add(page, button(this, "Open preparation checklist", false, v -> switchTab(2)), 12);
        add(page, text(this, "External documentation opens in your browser. TrillRoot itself has no internet permission and downloads no root packages.", 13, MUTED, false), 16);
    }

    private void checklist(LinearLayout page) {
        add(page, eyebrow(this, "PREPARATION / 03"), 4);
        add(page, heading(this, "Before you change\nanything.", 32), 12);
        add(page, text(this, "Save your progress as you prepare. Checking these boxes does not unlock or modify your device.", 15, MUTED, false), 12);
        Set<String> checked = checkedItems();
        LinearLayout progressCard = card(this);
        TextView count = heading(this, checked.size() + " of " + Checklist.ITEMS.size() + " prepared", 23);
        add(progressCard, count, 0);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Checklist.ITEMS.size());
        progress.setProgress(checked.size());
        progress.setProgressTintList(ColorStateList.valueOf(ACCENT));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(STROKE));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(this, 8));
        barLp.topMargin = dp(this, 18);
        progressCard.addView(progress, barLp);
        TextView progressNote = text(this, "Preparation is not a compatibility guarantee. Stop if official unlocking is unavailable.", 13, MUTED, false);
        add(progressCard, progressNote, 14);
        add(page, progressCard, 20);
        for (Checklist.Item item : Checklist.ITEMS) {
            LinearLayout box = card(this);
            CheckBox check = new CheckBox(this);
            check.setText(item.title);
            check.setTextSize(16);
            check.setTextColor(TEXT);
            check.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            check.setButtonTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                    new int[]{ACCENT, MUTED}));
            check.setMinHeight(dp(this, 48));
            check.setChecked(checked.contains(item.id));
            check.setOnCheckedChangeListener((v, isChecked) -> {
                Set<String> values = checkedItems();
                if (isChecked) values.add(item.id); else values.remove(item.id);
                preferences.edit().putStringSet(CHECKED, values).apply();
                count.setText(values.size() + " of " + Checklist.ITEMS.size() + " prepared");
                progress.setProgress(values.size());
            });
            add(box, check, 0);
            TextView detail = text(this, item.detail, 13, MUTED, false);
            detail.setPadding(dp(this, 32), 0, 0, 0);
            add(box, detail, 5);
            add(page, box, 12);
        }
        add(page, button(this, "Export readiness report", true, v -> exportReport()), 20);
        add(page, button(this, "Reset checklist", false, v -> confirmReset()), 10);
        add(page, text(this, "Checklist progress stays in this app's private storage. A report is written only to a destination you choose.", 13, MUTED, false), 14);
    }

    private void about(LinearLayout page) {
        add(page, eyebrow(this, "TRANSPARENCY / 04"), 4);
        add(page, heading(this, "Your device.\nYour decision.", 32), 12);
        add(page, text(this, "Trill Root is an independent root-readiness assistant, not a one-click exploit tool.", 15, MUTED, false), 12);
        LinearLayout version = card(this);
        add(version, heading(this, "Trill Root 0.2.0", 23), 0);
        add(version, text(this, "Preview build · Android 6.0+ (API 23) · Targets API 36", 13, CYAN, false), 8);
        detail(version, "Network permission", "None");
        detail(version, "Dangerous permissions", "None");
        detail(version, "Root binaries bundled", "None");
        detail(version, "Flashing / unlocking", "Never");
        detail(version, "Analytics / advertising", "None");
        add(page, version, 20);
        notice(page, "What the root check actually does", "After your confirmation, the app asks the existing su program to run id. UID 0 means access was granted. Failure is not proof that the device has no root. No alternative elevation method is tried.", CYAN);
        notice(page, "No universal-root promise", "A locked or policy-restricted device may have no supported rooting route. Towelroot, KingRoot, KingoRoot, Sunshine, historical MediaTek chains, and similar exploits are not bundled or executed: patched legacy chains are not safe fallbacks for an unknown device.", WARNING);
        notice(page, "Private by design", "No accounts, contacts, location, serial number, or IMEI are collected. The optional report includes model and build information. Review it before sharing. Checklist preferences remain local and are excluded from backup.", CYAN);
        add(page, heading(this, "Primary documentation", 22), 24);
        add(page, button(this, "Android bootloader unlock design", false, v -> openUrl(RouteEngine.AOSP_UNLOCK)), 12);
        add(page, button(this, "Magisk installation guide", false, v -> openUrl(RouteEngine.MAGISK_INSTALL)), 10);
        add(page, button(this, "KernelSU installation guide", false, v -> openUrl(RouteEngine.KERNELSU_INSTALL)), 10);
        add(page, button(this, "Android platform-tools", false,
                v -> openUrl("https://developer.android.com/tools/releases/platform-tools")), 10);
        add(page, button(this, "Review Terms of Use & legal notice", false, v -> showTerms(true)), 20);
        add(page, button(this, "Open Developer options", false, v -> openSettings()), 10);
        add(page, button(this, "Export readiness report", false, v -> exportReport()), 10);
        add(page, text(this, "Not affiliated with Magisk, KernelSU, Samsung, Google, or KingRoot. Rooting can affect warranty, updates, attestation, protected apps, and data. Use only devices you own or are explicitly authorized to modify.", 13, MUTED, false), 20);
    }

    private Route findRoute() {
        if (routeId == null) return null;
        for (Route route : RouteEngine.ordered(info, rootAccess)) if (route.id.equals(routeId)) return route;
        return null;
    }

    private void switchTab(int index) {
        tab = index;
        routeId = null;
        render();
    }

    private Set<String> checkedItems() {
        Set<String> stored = preferences.getStringSet(CHECKED, new HashSet<>());
        Set<String> result = new HashSet<>();
        for (Checklist.Item item : Checklist.ITEMS) if (stored.contains(item.id)) result.add(item.id);
        return result;
    }

    private void refreshDevice() {
        if (loading) return;
        loading = true;
        final int token = generation.get();
        render();
        worker.execute(() -> {
            DeviceInfo collected = null;
            try { collected = collector.collect(); }
            catch (RuntimeException ignored) { }
            final DeviceInfo result = collected;
            main.post(() -> {
                if (token != generation.get() || isFinishing() || isDestroyed()) return;
                loading = false;
                if (result != null) info = result;
                render();
                if (result == null) toast("Could not read device information. You can still browse the guides.");
            });
        });
    }

    private void confirmRootProbe() {
        if (probing || loading) return;
        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Check existing root access?")
                .setMessage("TrillRoot will run the read-only command “su -c id”. Your existing root manager may ask you to allow or deny it.\n\nThis does not install root, unlock the bootloader, or change partitions. If access is unavailable or denied, the app stops and shows guidance instead of trying exploits.\n\nContinue only on a device you control.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run read-only check", (dialog, which) -> startRootProbe())
                .show();
    }

    private void startRootProbe() {
        if (probing) return;
        probing = true;
        rootAccess = new RootAccess(RootAccess.State.CHECKING,
                "Waiting up to 20 seconds for your existing root manager. No changes are being made.");
        final int token = generation.get();
        render();
        worker.execute(() -> {
            RootAccess collected;
            try { collected = collector.probeRoot(); }
            catch (RuntimeException e) {
                collected = new RootAccess(RootAccess.State.ERROR, "The root check could not complete. No alternative method was attempted.");
            }
            final RootAccess result = collected;
            main.post(() -> {
                if (token != generation.get() || isFinishing() || isDestroyed()) return;
                probing = false;
                rootAccess = result;
                render();
            });
        });
    }

    private void confirmReset() {
        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Reset preparation checklist?")
                .setMessage("This clears only TrillRoot's saved checklist. It does not change your device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d, which) -> {
                    preferences.edit().remove(CHECKED).apply();
                    render();
                }).show();
    }

    private void exportReport() {
        if (info == null) { toast("Wait for device information, then try again."); return; }
        pendingReport = ReportFormatter.format(info, rootAccess, checkedItems());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TITLE, "trillroot-readiness.txt");
        try { startActivityForResult(intent, EXPORT_REQUEST); }
        catch (ActivityNotFoundException e) {
            pendingReport = null;
            toast("No document picker is available on this device.");
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != EXPORT_REQUEST) return;
        String report = pendingReport;
        pendingReport = null;
        if (result != RESULT_OK || data == null || data.getData() == null || report == null) return;
        Uri destination = data.getData();
        try (OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) throw new java.io.IOException("Destination is not writable");
            output.write(report.getBytes("UTF-8"));
            toast("Readiness report saved to your chosen destination.");
        } catch (Exception e) {
            toast("Could not save the report. Choose another destination.");
        }
    }

    private void copyCommand(String command) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) { toast("Clipboard is unavailable."); return; }
        clipboard.setPrimaryClip(ClipData.newPlainText("TrillRoot read-only PC command", command));
        toast("Read-only command copied. Run it on your computer when appropriate.");
    }

    private void openUrl(String value) {
        Uri uri = Uri.parse(value);
        Set<String> allowed = new HashSet<>();
        allowed.add("github.com");
        allowed.add("kernelsu.org");
        allowed.add("source.android.com");
        allowed.add("developer.android.com");
        if (!"https".equals(uri.getScheme()) || !allowed.contains(uri.getHost())) {
            toast("This documentation address is not allowed.");
            return;
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) { toast("No browser is available. Documentation: " + value); }
        catch (SecurityException e) { toast("Device policy blocked the browser. TrillRoot will not bypass it."); }
    }

    private void openSettings() {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)); }
        catch (ActivityNotFoundException e) {
            toast("Enable Developer options in Settings first, then try again.");
        } catch (SecurityException e) {
            toast("Device policy restricts access to Developer options. TrillRoot will not bypass it.");
        }
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    private void handleBack() {
        if (onboarding) { finish(); return; }
        if (reviewingTerms) { reviewingTerms = false; buildShell(); render(); return; }
        if (routeId != null) { routeId = null; render(); }
        else if (tab != 0) switchTab(0);
        else finish();
    }

    @Override public void onBackPressed() {
        if (Build.VERSION.SDK_INT < 33) handleBack();
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("tab", tab);
        out.putString("route", routeId);
        out.putString("pendingReport", pendingReport);
        super.onSaveInstanceState(out);
    }

    @Override protected void onDestroy() {
        generation.incrementAndGet();
        if (activeDialog != null) activeDialog.dismiss();
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        worker.shutdownNow();
        runner.close();
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
