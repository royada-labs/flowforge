import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';
import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">The Code-First Reactive Workflow Engine for Java & Spring Boot.</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/getting-started">
            Get Started ⚡
          </Link>
          <Link
            className="button button--outline button--secondary button--lg"
            style={{marginLeft: '20px'}}
            to="/docs/tutorial">
            Interactive Tutorial 🛠️
          </Link>
        </div>
      </div>
    </header>
  );
}

function Section({title, children, className}) {
  return (
    <section className={clsx(styles.section, className)}>
      <div className="container">
        <Heading as="h2">{title}</Heading>
        <div className={styles.sectionContent}>
          {children}
        </div>
      </div>
    </section>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} | Reactive Workflow Engine`}
      description="FlowForge is a high-performance orchestration engine for Spring Boot and Project Reactor.">
      <HomepageHeader />
      <main>
        <Section title="What is FlowForge?">
          <p>
            FlowForge is a library designed to orchestrate complex, asynchronous, and reactive workflows in Java. 
            Built on top of <strong>Project Reactor</strong>, it allows developers to define business logic as a set of 
            independent tasks and compose them into resilient, type-safe execution pipelines.
          </p>
        </Section>

        <Section title="What it brings to your project" className={styles.sectionAlt}>
          <div className="row">
            <div className="col col--4">
              <h3>Type Safety</h3>
              <p>Forget about string-based keys. Everything in FlowForge is strongly typed, ensuring that your workflow components talk to each other correctly at compile time.</p>
            </div>
            <div className="col col--4">
              <h3>Built-in Resilience</h3>
              <p>Automatic retries, timeouts, and fallback mechanisms are first-class citizens. Build reliable systems without bloating your business logic.</p>
            </div>
            <div className="col col--4">
              <h3>Reactive & Efficient</h3>
              <p>Non-blocking by design. FlowForge integrates seamlessly with WebFlux and R2DBC to handle massive amounts of concurrent executions with minimal resources.</p>
            </div>
          </div>
        </Section>

        <Section title="What FlowForge is NOT">
          <ul style={{fontSize: '1.2rem', lineHeight: '2rem'}}>
            <li><strong>NOT a BPMS:</strong> We don't use heavy XML or BPMN files. Your workflow IS your code.</li>
            <li><strong>NOT a Low-Code tool:</strong> It is designed for developers who love clean, versionable, and testable code.</li>
            <li><strong>NOT a Distributed Scheduler:</strong> It manages execution within your application instance via Reactor, not external cron jobs.</li>
          </ul>
        </Section>
      </main>
    </Layout>
  );
}
