import "server-only";
import { Resend } from "resend";
import { getEnv } from "./env";

/**
 * Sends a single-use OTP email via Resend.
 * The API key is securely loaded from server environment variables only.
 */
export async function sendOtpEmail(to: string, otp: string): Promise<{ success: boolean; error?: string }> {
  const env = getEnv();

  // If in local development or test with a mock key, log safely without exposing secrets
  if (
    !env.RESEND_API_KEY ||
    env.RESEND_API_KEY.startsWith("re_placeholder") ||
    env.RESEND_API_KEY === "re_mock_test_key"
  ) {
    if (process.env.NODE_ENV !== "production") {
      console.info(`[Auth Dev OTP] Verification code for ${to}: ${otp}`);
    } else {
      console.info(`[Auth Email] Verification code generated for ${to} (mock key active)`);
    }
    return { success: true };
  }

  try {
    const resend = new Resend(env.RESEND_API_KEY);
    const result = await resend.emails.send({
      from: env.EMAIL_FROM,
      to,
      subject: "Your Teaching Planner Verification Code",
      text: `Your six-digit verification code is: ${otp}\n\nThis code expires in 5 minutes. If you did not request this code, you can safely ignore this email.`,
      html: `
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; color: #1e2022;">
          <h2 style="font-size: 20px; font-weight: 600; margin-bottom: 16px;">Teaching Planner Verification Code</h2>
          <p style="font-size: 14px; line-height: 1.5; color: #4b5563;">Use the six-digit code below to complete your sign-in to the faculty workspace:</p>
          <div style="background: #f4f2ec; padding: 18px 24px; border-radius: 8px; text-align: center; margin: 24px 0; letter-spacing: 6px; font-size: 32px; font-weight: 700; color: #1b2820;">
            ${otp}
          </div>
          <p style="font-size: 13px; color: #6b7280; line-height: 1.4;">This code expires in <strong>5 minutes</strong>. For security, never share this code with anyone.</p>
          <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;" />
          <p style="font-size: 11px; color: #9ca3af;">If you did not request this login code, you can safely ignore this email.</p>
        </div>
      `,
    });

    if (result.error) {
      console.error("[Auth Email] Resend delivery error:", result.error.message);
      return { success: false, error: result.error.message };
    }

    return { success: true };
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to deliver email";
    console.error("[Auth Email] Unexpected exception during email delivery:", message);
    return { success: false, error: message };
  }
}
