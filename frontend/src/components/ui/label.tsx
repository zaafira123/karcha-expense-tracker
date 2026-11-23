import * as LabelPrimitive from '@radix-ui/react-label'
import type { ComponentProps } from 'react'
export function Label(props: ComponentProps<typeof LabelPrimitive.Root>) { return <LabelPrimitive.Root {...props} className={`small text-secondary ${props.className ?? ''}`} /> }
