import * as TabsPrimitive from '@radix-ui/react-tabs'
import type { ComponentProps } from 'react'
export const Tabs = (props: ComponentProps<typeof TabsPrimitive.Root>) => <TabsPrimitive.Root {...props} />
export function TabsList(props: ComponentProps<typeof TabsPrimitive.List>) { return <TabsPrimitive.List {...props} className={`flex border-b border-border ${props.className ?? ''}`} /> }
export function TabsTrigger(props: ComponentProps<typeof TabsPrimitive.Trigger>) { return <TabsPrimitive.Trigger {...props} className={`flex-1 border-b-2 border-transparent pb-3 text-small text-muted data-[state=active]:border-accent data-[state=active]:font-medium data-[state=active]:text-accent ${props.className ?? ''}`} /> }
export function TabsContent(props: ComponentProps<typeof TabsPrimitive.Content>) { return <TabsPrimitive.Content {...props} /> }
