import AuthForm from "@/components/auth-form";

export default async function LoginPage({ searchParams }: {
  searchParams: Promise<{ registered?: string }>;
}) {
  const { registered } = await searchParams;
  return <AuthForm mode="login" registered={registered === "1"} />;
}
